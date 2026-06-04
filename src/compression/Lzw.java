package compression;

import java.io.*;
import java.util.*;

public class Lzw {

    public byte[] compress(byte[] input) throws IOException {

        Map<String, Integer> dictionary = new HashMap<>();

        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        int dictSize = 256;
        String w = "";

        List<Integer> result = new ArrayList<>();

        for (byte b : input) {

            char c = (char) (b & 0xFF);

            String wc = w + c;

            if (dictionary.containsKey(wc)) {
                w = wc;
            } else {

                result.add(dictionary.get(w));

                dictionary.put(wc, dictSize++);

                w = "" + c;
            }
        }

        if (!w.isEmpty()) {
            result.add(dictionary.get(w));
        }

        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();

        DataOutputStream dos =
                new DataOutputStream(baos);

        for (int code : result) {
            dos.writeInt(code);
        }

        dos.flush();

        return baos.toByteArray();
    }

    public byte[] decompress(byte[] compressed)
            throws IOException {

        DataInputStream in =
                new DataInputStream(
                        new ByteArrayInputStream(compressed)
                );

        List<Integer> codes = new ArrayList<>();

        while (in.available() > 0) {
            codes.add(in.readInt());
        }

        if (codes.isEmpty()) {
            return new byte[0];
        }

        Map<Integer, String> dictionary =
                new HashMap<>();

        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        int dictSize = 256;

        String w =
                dictionary.get(codes.remove(0));

        StringBuilder result =
                new StringBuilder(w);

        for (int k : codes) {

            String entry;

            if (dictionary.containsKey(k)) {
                entry = dictionary.get(k);
            } else if (k == dictSize) {
                entry = w + w.charAt(0);
            } else {
                throw new IOException(
                        "Codigo LZW invalido: " + k
                );
            }

            result.append(entry);

            dictionary.put(
                    dictSize++,
                    w + entry.charAt(0)
            );

            w = entry;
        }

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        for (char c : result.toString().toCharArray()) {
            out.write((byte) c);
        }

        return out.toByteArray();
    }
}