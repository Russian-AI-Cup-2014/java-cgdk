import model.*;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class RemoteProcessClient implements Closeable {
    private static final int BUFFER_SIZE_BYTES = 1 << 20;
    private static final ByteOrder PROTOCOL_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int INTEGER_SIZE_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int LONG_SIZE_BYTES = Long.SIZE / Byte.SIZE;

    private final Socket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final ByteArrayOutputStream outputStreamBuffer;

    public RemoteProcessClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setSendBufferSize(BUFFER_SIZE_BYTES);
        socket.setReceiveBufferSize(BUFFER_SIZE_BYTES);
        socket.setTcpNoDelay(true);

        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        outputStreamBuffer = new ByteArrayOutputStream(BUFFER_SIZE_BYTES);
    }

    public void writeToken(String token) throws IOException {
        writeEnum(MessageType.AUTHENTICATION_TOKEN);
        writeString(token);
        flush();
    }

    public int readTeamSize() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.TEAM_SIZE);
        return readInt();
    }

    public void writeProtocolVersion() throws IOException {
        writeEnum(MessageType.PROTOCOL_VERSION);
        writeInt(1);
        flush();
    }

    public Game readGameContext() throws IOException {
        ensureMessageType(readEnum(MessageType.class), MessageType.GAME_CONTEXT);
        if (!readBoolean()) {
            return null;
        }

        return new Game(
                readLong(), readInt(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readInt(), readInt(), readInt(), readInt(),
                readInt(), readInt(), readDouble(), readDouble(), readDouble(), readInt(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readInt(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readInt(), readInt(), readInt(), readInt(), readInt(),
                readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(), readInt(),
                readDouble(), readDouble()
        );
    }

    public PlayerContext readPlayerContext() throws IOException {
        MessageType messageType = readEnum(MessageType.class);
        if (messageType == MessageType.GAME_OVER) {
            return null;
        }

        ensureMessageType(messageType, MessageType.PLAYER_CONTEXT);
        return readBoolean() ? new PlayerContext(readHockeyists(), readWorld()) : null;
    }

    public void writeMoves(Move[] moves) throws IOException {
        writeEnum(MessageType.MOVES);

        if (moves == null) {
            writeInt(-1);
        } else {
            int moveCount = moves.length;
            writeInt(moveCount);

            for (int moveIndex = 0; moveIndex < moveCount; ++moveIndex) {
                Move move = moves[moveIndex];

                if (move == null) {
                    writeBoolean(false);
                } else {
                    writeBoolean(true);

                    writeDouble(move.getSpeedUp());
                    writeDouble(move.getTurn());
                    writeEnum(move.getAction());
                    if (move.getAction() == ActionType.PASS) {
                        writeDouble(move.getPassPower());
                        writeDouble(move.getPassAngle());
                    } else if (move.getAction() == ActionType.SUBSTITUTE) {
                        writeInt(move.getTeammateIndex());
                    }
                }
            }
        }

        flush();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private World readWorld() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new World(
                readInt(), readInt(), readDouble(), readDouble(), readPlayers(), readHockeyists(), readPuck()
        );
    }

    private Player[] readPlayers() throws IOException {
        int playerCount = readInt();
        if (playerCount < 0) {
            return null;
        }

        Player[] players = new Player[playerCount];

        for (int playerIndex = 0; playerIndex < playerCount; ++playerIndex) {
            if (readBoolean()) {
                players[playerIndex] = new Player(
                        readLong(), readBoolean(), readString(), readInt(), readBoolean(),
                        readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                        readBoolean(), readBoolean()
                );
            }
        }

        return players;
    }

    private Hockeyist[] readHockeyists() throws IOException {
        int hockeyistCount = readInt();
        if (hockeyistCount < 0) {
            return null;
        }

        Hockeyist[] hockeyists = new Hockeyist[hockeyistCount];

        for (int hockeyistIndex = 0; hockeyistIndex < hockeyistCount; ++hockeyistIndex) {
            hockeyists[hockeyistIndex] = readHockeyist();
        }

        return hockeyists;
    }

    private Hockeyist readHockeyist() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Hockeyist(
                readLong(), readLong(), readInt(), readDouble(), readDouble(), readDouble(), readDouble(),
                readDouble(), readDouble(), readDouble(), readDouble(), readBoolean(), readEnum(HockeyistType.class),
                readInt(), readInt(), readInt(), readInt(), readDouble(), readEnum(HockeyistState.class),
                readInt(), readInt(), readInt(), readInt(), readEnum(ActionType.class), readBoolean() ? readInt() : null
        );
    }

    private Puck readPuck() throws IOException {
        if (!readBoolean()) {
            return null;
        }

        return new Puck(
                readLong(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(), readDouble(),
                readLong(), readLong()
        );
    }

    private static void ensureMessageType(MessageType actualType, MessageType expectedType) {
        if (actualType != expectedType) {
            throw new IllegalArgumentException(String.format(
                    "Received wrong message [actual=%s, expected=%s].", actualType, expectedType
            ));
        }
    }

    private <E extends Enum> E readEnum(Class<E> enumClass) throws IOException {
        byte ordinal = readBytes(1)[0];

        E[] values = enumClass.getEnumConstants();
        int valueCount = values.length;

        for (int valueIndex = 0; valueIndex < valueCount; ++valueIndex) {
            E value = values[valueIndex];
            if (value.ordinal() == ordinal) {
                return value;
            }
        }

        return null;
    }

    private <E extends Enum> void writeEnum(E value) throws IOException {
        writeBytes(new byte[]{value == null ? (byte) -1 : (byte) value.ordinal()});
    }

    private String readString() throws IOException {
        int length = readInt();
        if (length == -1) {
            return null;
        }

        try {
            return new String(readBytes(length), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UTF-8 is unsupported.", e);
        }
    }

    private void writeString(String value) throws IOException {
        if (value == null) {
            writeInt(-1);
            return;
        }

        byte[] bytes;
        try {
            bytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("UTF-8 is unsupported.", e);
        }

        writeInt(bytes.length);
        writeBytes(bytes);
    }

    private boolean readBoolean() throws IOException {
        return readBytes(1)[0] != 0;
    }

    private boolean[] readBooleanArray(int count) throws IOException {
        byte[] bytes = readBytes(count);
        boolean[] booleans = new boolean[count];

        for (int i = 0; i < count; ++i) {
            booleans[i] = bytes[i] != 0;
        }

        return booleans;
    }

    private void writeBoolean(boolean value) throws IOException {
        writeBytes(new byte[]{value ? (byte) 1 : (byte) 0});
    }

    private int readInt() throws IOException {
        return ByteBuffer.wrap(readBytes(INTEGER_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getInt();
    }

    private void writeInt(int value) throws IOException {
        writeBytes(ByteBuffer.allocate(INTEGER_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putInt(value).array());
    }

    private long readLong() throws IOException {
        return ByteBuffer.wrap(readBytes(LONG_SIZE_BYTES)).order(PROTOCOL_BYTE_ORDER).getLong();
    }

    private void writeLong(long value) throws IOException {
        writeBytes(ByteBuffer.allocate(LONG_SIZE_BYTES).order(PROTOCOL_BYTE_ORDER).putLong(value).array());
    }

    private double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    private byte[] readBytes(int byteCount) throws IOException {
        byte[] bytes = new byte[byteCount];
        int offset = 0;
        int readByteCount;

        while (offset < byteCount && (readByteCount = inputStream.read(bytes, offset, byteCount - offset)) != -1) {
            offset += readByteCount;
        }

        if (offset != byteCount) {
            throw new IOException(String.format("Can't read %d bytes from input stream.", byteCount));
        }

        return bytes;
    }

    private void writeBytes(byte[] bytes) throws IOException {
        outputStreamBuffer.write(bytes);
    }

    private void flush() throws IOException {
        outputStream.write(outputStreamBuffer.toByteArray());
        outputStreamBuffer.reset();
        outputStream.flush();
    }

    private enum MessageType {
        UNKNOWN,
        GAME_OVER,
        AUTHENTICATION_TOKEN,
        TEAM_SIZE,
        PROTOCOL_VERSION,
        GAME_CONTEXT,
        PLAYER_CONTEXT,
        MOVES
    }
}
