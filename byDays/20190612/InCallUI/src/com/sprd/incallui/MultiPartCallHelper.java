package com.sprd.incallui;

public class MultiPartCallHelper {
    /**
     * MPC (Multi-Part-Call) mode: hang up background call and accept ringing/waiting call.
     */
    public static final int MPC_MODE_HB = 0;

    /**
     * MPC (Multi-Part-Call) mode: hang up foreground call and accept ringing/waiting call.
     */
    public static final int MPC_MODE_HF = 1;
    /**
     * MPC (Multi-Part-Call) mode: hang up foreground call and accept ringing/waiting call (two call case).
     */
    public static final int MPC_MODE_HF_TWO= 2;
}
