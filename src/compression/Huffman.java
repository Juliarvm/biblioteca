package compression;

import java.io.*;
import java.util.*;

public class Huffman {

    private static class Node implements Comparable<Node> {

        byte symbol;
        int frequency;

        Node left;
        Node right;

        Node(byte symbol, int frequency) {
            this.symbol = symbol;
            this.frequency = frequency;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.frequency = left.frequency + right.frequency;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.frequency, o.frequency);
        }
    }

    private Map<Byte, String> buildTable(Node root) {

        Map<Byte, String> table = new HashMap<>();

        buildCodes(root, "", table);

        return table;
    }

    private void buildCodes(Node node,
                            String code,
                            Map<Byte, String> table) {

        if (node == null)
            return;

        if (node.isLeaf()) {

            table.put(
                    node.symbol,
                    code.isEmpty() ? "0" : code
            );

            return;
        }

        buildCodes(node.left, code + "0", table);
        buildCodes(node.right, code + "1", table);
    }

    private Node buildTree(Map<Byte,Integer> frequencies) {

        PriorityQueue<Node> queue =
                new PriorityQueue<>();

        for (var e : frequencies.entrySet()) {

            queue.add(
                    new Node(
                            e.getKey(),
                            e.getValue()
                    )
            );
        }

        if (queue.size() == 1) {

            Node only = queue.poll();

            return new Node(
                    only,
                    new Node((byte)0, 0)
            );
        }

        while (queue.size() > 1) {

            Node left = queue.poll();
            Node right = queue.poll();

            queue.add(new Node(left, right));
        }

        return queue.poll();
    }

    public byte[] compress(byte[] data) throws IOException {
	System.out.println("Compressao iniciou (em Huffman.java)");
        if (data.length == 0)
            return new byte[0];

        Map<Byte,Integer> frequencies =
                new HashMap<>();

        for (byte b : data) {

            frequencies.put(
                    b,
                    frequencies.getOrDefault(b, 0) + 1
            );
        }

        Node root = buildTree(frequencies);

        Map<Byte,String> table =
                buildTable(root);

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        DataOutputStream out =
                new DataOutputStream(baos);

        out.writeInt(frequencies.size());

        for (var e : frequencies.entrySet()) {

            out.writeByte(e.getKey());
            out.writeInt(e.getValue());
        }

        out.writeInt(data.length);

        int currentByte = 0;
        int bitsFilled = 0;

        for (byte b : data) {

            String code = table.get(b);

            for (char bit : code.toCharArray()) {

                currentByte <<= 1;

                if (bit == '1')
                    currentByte |= 1;

                bitsFilled++;

                if (bitsFilled == 8) {

                    out.writeByte(currentByte);

                    currentByte = 0;
                    bitsFilled = 0;
                }
            }
        }

        if (bitsFilled > 0) {

            currentByte <<= (8 - bitsFilled);

            out.writeByte(currentByte);
        }

        out.flush();

        return baos.toByteArray();
    }

    public byte[] decompress(byte[] compressed)
throws IOException {

        if (compressed.length == 0)
            return new byte[0];

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(compressed)
                );

        int mapSize = in.readInt();

        Map<Byte,Integer> frequencies =
                new HashMap<>();

        for (int i = 0; i < mapSize; i++) {

            byte symbol = in.readByte();
            int freq = in.readInt();

            frequencies.put(symbol, freq);
        }

        int originalLength = in.readInt();

        Node root = buildTree(frequencies);

        ByteArrayOutputStream result =
                new ByteArrayOutputStream();

        Node current = root;

        while (in.available() > 0 &&
               result.size() < originalLength) {

            int value = in.readUnsignedByte();

            for (int i = 7; i >= 0; i--) {

                int bit = (value >> i) & 1;

                current =
                        bit == 0
                                ? current.left
                                : current.right;

                if (current.isLeaf()) {

                    result.write(current.symbol);

                    current = root;

                    if (result.size() ==
                        originalLength)
                        break;
                }
            }
        }

        return result.toByteArray();
    }
}
