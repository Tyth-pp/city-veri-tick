package com.city.integration;

/**
 * Extension point for a future QR-code provider.
 * 这是一个QR码渲染器的接口，作为未来QR码提供程序的扩展点。
 */
public interface QrCodeRenderer {

    /**
     * 将内容渲染为QR码并返回字节数组
     * @param content 要渲染为QR码的内容字符串
     * @return 包含QR码图像数据的字节数组
     */
    byte[] render(String content); // 接口方法，将字符串内容渲染为QR码的字节数组
}
