package com.unisec.talkback;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

public class Talkback {

	/**
	 * 常量定义
	 */
	private static final int AUDIO_FRAME_SIZE = 9920;
	private static final int AUDIO_CODE_BUFFER_SIZE = 500;

	/**
	 * 音频编码变量
	 * 
	 * @author zrf
	 *
	 */
	private class AudioEncode {
		public int encodeContext = -1;
		public AudioRecord audioRecord = null;
		public byte[] recordBuffer = null;
		public RecordThread recordThread = null;
		public byte[] encodeBuffer = null;
	}

	/**
	 * 音频解码变量
	 * 
	 * @author zrf
	 *
	 */
	private class AudioDecode {
		public int decodeContext = -1;
		public AudioTrack audioTrack = null;
		public byte[] wavOutBuffer = null;
		public AudioDecoder audioDecoder = null;
	}

	/**
	 * 解码队列元素
	 * 
	 * @author zrf
	 *
	 */
	private class DecodeQueue {
		public byte[] data;
		public int size;

		public DecodeQueue(int id, byte[] data, int size) {
			this.data = data;
			this.size = size;
		}
	}

	/**
	 * 打开录音设备
	 * 
	 * @return 成功返回true
	 */
	public boolean openAudioDevice() {
		// 关闭之前打开的设备
		closeAudioDevice();
		// 打开音频输入
		int inBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		AudioEncode audioEncode = new AudioEncode();
		audioEncode.audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, inBufferSize * 60);
		audioEncode.recordBuffer = new byte[AUDIO_FRAME_SIZE];
		audioEncode.encodeBuffer = new byte[AUDIO_CODE_BUFFER_SIZE];
		audioEncode.encodeBuffer[0] = (byte) ((mStreamId >> 24) & 0xFF);
		audioEncode.encodeBuffer[1] = (byte) ((mStreamId >> 16) & 0xFF);
		audioEncode.encodeBuffer[2] = (byte) ((mStreamId >> 8) & 0xFF);
		audioEncode.encodeBuffer[3] = (byte) (mStreamId & 0xFF);
		// 初始化音频编码
		audioEncode.encodeContext = NativeSupport.AudioEncodeInit();
		audioEncode.recordThread = new RecordThread();
		try {
			audioEncode.audioRecord.startRecording();
			mAudioEncode = audioEncode;
		} catch (Exception ex) {
			ex.printStackTrace();
			audioEncode.audioRecord.release();
			audioEncode.audioRecord = null;
			return false;
		}
		new Thread(mAudioEncode.recordThread).start();
		return true;
	}

	/**
	 * 关闭录音设备
	 */
	public void closeAudioDevice() {
		if (mAudioEncode != null) {
			AudioEncode audioEncode = mAudioEncode;
			mAudioEncode = null;
			if (audioEncode.audioRecord != null) {
				audioEncode.audioRecord.stop();
				audioEncode.audioRecord.release();
			}
			NativeSupport.AudioEncodeDestroy(audioEncode.encodeContext);
		}
	}

	/**
	 * 打开音频输出
	 * 
	 * @return 成功返回true
	 */
	public boolean openAudioOutput() {
		// 关闭之前打开的输出
		closeAudioOutput();
		// 打开音频输出
		int outBufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		AudioDecode audioDecode = new AudioDecode();
		audioDecode.audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, outBufferSize, AudioTrack.MODE_STREAM);
		audioDecode.audioTrack.play();
		// 初始化音频解码
		audioDecode.decodeContext = NativeSupport.AudioDecodeInit();
		audioDecode.wavOutBuffer = new byte[AUDIO_FRAME_SIZE];
		audioDecode.audioDecoder = new AudioDecoder();
		mAudioDecode = audioDecode;
		return true;
	}

	/**
	 * 关闭录音设备
	 */
	public void closeAudioOutput() {
		// 关闭音频输出
		if (mAudioDecode != null) {
			if (mAudioDecode.audioTrack != null) {
				mAudioDecode.audioTrack.stop();
				mAudioDecode.audioTrack.release();
			}
			mAudioDecode.audioDecoder.stop();
			NativeSupport.AudioDecodeDestroy(mAudioDecode.decodeContext);
			mAudioDecode = null;
		}
	}

	/**
	 * 构造函数
	 * 
	 * @param address
	 * @throws Exception
	 */
	public Talkback(String address) throws Exception {
		int idx1 = address.indexOf("//");
		int idx2 = address.indexOf(':', idx1);
		int idx3 = address.indexOf('/', idx2);
		String ip = address.substring(idx1 + 2, idx2);
		String port = address.substring(idx2 + 1, idx3);
		String streamId = address.substring(idx3 + 1);
		mAddress = InetAddress.getByName(ip);
		mPort = Integer.parseInt(port);
		mStreamId = Integer.parseInt(streamId);
		mSocket = new DatagramSocket();
	}

	// UDP连接
	private DatagramSocket mSocket = null;
	// 服务器IP地址
	private InetAddress mAddress = null;
	// 服务器端口
	private int mPort = 0;
	// 对讲流ID
	private int mStreamId = 0;
	// 音频编码上下文
	private AudioEncode mAudioEncode = null;
	// 音频解码上下文
	private AudioDecode mAudioDecode = null;

	/**
	 * 处理对讲信息
	 * 
	 * @param destinationID
	 * @param data
	 * @param size
	 */
	public void processMsg(long destinationID, byte[] data, int size) {
		if (mAudioDecode != null && size <= AUDIO_CODE_BUFFER_SIZE) {
			mAudioDecode.audioDecoder.decode(0, data, size);
		}
	}

	/**
	 * 音频解码输出
	 * 
	 * @author zrf
	 *
	 */
	private class AudioDecoder {
		// 数据缓冲队列
		private ArrayList<DecodeQueue> dataQueue = new ArrayList<DecodeQueue>();
		// 解码线程
		private DecodeThread decodeThread = null;

		/**
		 * 构造函数
		 */
		public AudioDecoder() {
			decodeThread = new DecodeThread();
			decodeThread.start();
		}

		/**
		 * 停止解码
		 */
		public void stop() {
			decodeThread.alive = false;
			try {
				decodeThread.join();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		/**
		 * 解码 AMR 数据帧
		 * 
		 * @param id
		 * @param data
		 * @param size
		 */
		public void decode(int id, byte[] data, int size) {
			synchronized (dataQueue) {
				if (dataQueue.size() < 10) {
					if (dataQueue.size() == 0) {
						dataQueue.add(new DecodeQueue(id, data, size));
						dataQueue.notify();
					} else {
						dataQueue.add(new DecodeQueue(id, data, size));
					}
				}
			}
		}

		/**
		 * 解码线程
		 * 
		 * @author zrf
		 *
		 */
		private class DecodeThread extends Thread {
			public boolean alive = false;

			public void run() {
				alive = true;
				while (alive) {
					DecodeQueue item = null;
					synchronized (dataQueue) {
						if (dataQueue.size() == 0) {
							try {
								dataQueue.wait();
							} catch (InterruptedException ie) {
								continue;
							}
						}
						item = dataQueue.remove(0);
					}
					if (NativeSupport.AudioDecodeFeed(mAudioDecode.decodeContext, item.data, 4, item.size, mAudioDecode.wavOutBuffer, 0,
							AUDIO_FRAME_SIZE) > 0) {
						mAudioDecode.audioTrack.write(mAudioDecode.wavOutBuffer, 0, AUDIO_FRAME_SIZE);
					}
				}
			}
		}
	}

	/**
	 * 获取录音数据线程
	 * 
	 * @author zrf
	 *
	 */
	private class RecordThread implements Runnable {
		@Override
		public void run() {
			while (mAudioEncode != null) {
				try {
					if (mAudioEncode.audioRecord.read(mAudioEncode.recordBuffer, 0, AUDIO_FRAME_SIZE) == AUDIO_FRAME_SIZE) {
						int length = NativeSupport.AudioEncodeFeed(mAudioEncode.encodeContext, mAudioEncode.recordBuffer, 0,
								AUDIO_FRAME_SIZE, mAudioEncode.encodeBuffer, 4, AUDIO_CODE_BUFFER_SIZE - 4);
						if (length > 0) {
							mSocket.send(new DatagramPacket(mAudioEncode.encodeBuffer, length + 4, mAddress, mPort));
						}
					}
					Thread.sleep(1);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}