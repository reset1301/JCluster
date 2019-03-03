package ru.rrr.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Утилитный класс для работы с потоками
 */
public class IOHelper {
    /**
     * Читает {@link InputStream} в массив байт
     *
     * @param inputStream входящий поток
     * @return byte[] массив байт
     * @throws IOException ошибка чтения или создания исходящего потока
     */
    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        return toByteArray(inputStream, 1024);
    }

    /**
     * Читает {@link InputStream} в массив байт
     *
     * @param inputStream входящий поток
     * @param bufferSize  размер буфера для чтения
     * @return byte[] массив байт
     * @throws IOException ошибка чтения или создания исходящего потока
     */
    public static byte[] toByteArray(InputStream inputStream, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            while (inputStream.available() > 0) {
                int count = inputStream.read(buffer);
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        }
    }
}
