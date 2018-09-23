package com.unisec.talkback;

public class NativeSupport {
	static {
		System.loadLibrary("Talkback");
	}

	public static native int AudioEncodeInit();

	public static native int AudioEncodeFeed(int context, byte[] pcm, int iOffset, int iSize, byte[] amr, int oOffset, int oSize);

	public static native int AudioEncodeDestroy(int context);

	public static native int AudioDecodeInit();

	public static native int AudioDecodeFeed(int context, byte[] amr, int iOffset, int iSize, byte[] pcm, int oOffset, int oSize);

	public static native int AudioDecodeDestroy(int context);
}