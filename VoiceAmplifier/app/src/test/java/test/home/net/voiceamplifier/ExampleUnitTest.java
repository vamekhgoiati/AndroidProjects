package test.home.net.voiceamplifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void arrayTest() {
        short s = 10; //0000000000001010
        byte b1,b2;

        b1 = (byte) (s & 0xff);
        b2 = (byte) ((s >> 8) & 0xff);

        System.out.println((short) (s & 0xff));
        System.out.println((short) ((s >> 8) & 0xff));
        System.out.println(b1);
        System.out.println(b2);

        String str = "";
        for (int i = 0; i < 16; i++) {
            str = (s & 1) + str;
            s = (short) (s >> 1);
            System.out.println(str);
            System.out.println(s);
        }

        long a = 16;
        long b = (a >> 2);

        System.out.println(b);

        short t = 11;
        byte[] tB = short2Byte(t);
        System.out.println(byte2Short(tB[0], tB[1]));

    }

    @Test
    public void EndiannessTest() {
        int i = 10;
        byte[] b = new byte[2];
        b[0] = (byte) (i & 0xff);
        b[1] = (byte) ((i >> 8) & 0xff);

        double d1 = (double) ((b[0] << 8) | (b[1] & 0xff));
        double d2 = (double) ((b[0] & 0xff) | (b[1] << 8));

        System.out.println("d1 " + d1);
        System.out.println("d2 " + d2);
    }

    private short byte2Short(byte b1, byte b2) {
        return (short) ((b1 & 0xff) | (b2 << 8));
    }

    @Test
    public void wtf() {
        short[] s = new short[]{667, 776};
        long acc = 0;

        acc += ((long)(s[0] & 0xff)) << 48;
        acc += ((long)(s[1] & 0xff)) << 56;

        double sample = ((double) acc / (double) Long.MAX_VALUE);
        sample *= 2;
        int intValue = (int) (sample * (double) Integer.MAX_VALUE);

        for (int i = 0; i < 2; i++) {
            s[i] = (byte) (intValue >>> ((i + 2) * 8) & 0xff);
        }

        System.out.println("s[0] = " + s[0]);
        System.out.println("s[1] = " + s[1]);

    }

    @Test
    public void shiftTest() {
        int a = -10;
        System.out.println(Integer.toBinaryString(a >>> 1));
        System.out.println(a >>> 1);
        System.out.println(Integer.toBinaryString(a >> 1));
        System.out.println(a >> 1);
        System.out.println(Integer.MAX_VALUE);
        System.out.println(Integer.MIN_VALUE);
    }

    private byte[] short2Byte(short s) {
        byte[] res = new byte[2];
        res[0] = (byte) (s & 0xff);
        res[1] = (byte) ((s >> 8) & 0xff);

        return res;
    }
}