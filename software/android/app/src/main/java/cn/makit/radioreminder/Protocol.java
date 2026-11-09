package cn.makit.radioreminder;
import java.nio.*;
import java.util.UUID;
public final class Protocol {
 public static final UUID SERVICE=UUID.fromString("43710000-9d6f-4b8a-9d30-524144494f01");
 public static final UUID COMMAND=UUID.fromString("43710001-9d6f-4b8a-9d30-524144494f01");
 public static final UUID STATUS=UUID.fromString("43710002-9d6f-4b8a-9d30-524144494f01");
 public static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 public static final int SYNC=1, ALERT=2, STOP=3, PREVIEW=4, SOUND=5, VOLUME=6;
 public static byte[] packet(int op,int seq,int arg){return ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).put((byte)1).put((byte)op).putInt(seq).putInt(arg).array();}
 public static int arg(byte[] b){return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt(6);}
}
