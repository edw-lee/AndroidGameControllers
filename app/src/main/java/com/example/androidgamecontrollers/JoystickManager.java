package com.example.androidgamecontrollers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Timer;
import java.util.TimerTask;

/*
This manages all objects in the game and is responsible for
updating all states and render all objects to the screen
 */
class JoystickManager extends SurfaceView implements SurfaceHolder.Callback {
    private Joystick joystick;
    private AppLoop appLoop;
    private final int BYTE_MAX = 256;
    private final int BT_WRITE_INTERVAL = 250;

    Timer joystickBtWriteTimer = null;
    TimerTask joystickBtWriteTask = null;

    private BluetoothManager btManager;

    public JoystickManager(Context context, BluetoothManager btManager) {
        super(context);

        //Get surface holder and set callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        appLoop = new AppLoop(this, surfaceHolder);

        setFocusable(true);

        this.btManager = btManager;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        appLoop.startLoop();

        int joystickColor = getResources().getColor(R.color.joystickColor);
        int joystickBackground = getResources().getColor(R.color.joystickBackground);
        joystick = new Joystick(getPivotX(), getPivotY(), 150, 100, joystickColor, joystickBackground);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void draw(Canvas canvas) {
        if(canvas == null) {
            return;
        }

        super.draw(canvas);

        drawUPS(canvas);
        drawFPS(canvas);

        joystick.draw(canvas);
        drawJoystickActuatorValue(canvas);
    }

    public void drawUPS(Canvas canvas) {
        String averageUPS = Double.toString(appLoop.getAverageUPS());
        Paint paint = new Paint();
        int color = getResources().getColor(R.color.magenta);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("UPS: " + averageUPS, 100, 50, paint);
    }

    public void drawFPS(Canvas canvas) {
        String averageFPS = Double.toString(appLoop.getAverageFPS());
        Paint paint = new Paint();
        int color = ContextCompat.getColor(getContext(), R.color.magenta);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("FPS: " + averageFPS, 100, 100, paint);
    }

    public void drawJoystickActuatorValue(Canvas canvas) {
        String actuatorX = String.format("%.4f", joystick.getActuatorX());
        String actuatorY = String.format("%.4f", joystick.getActuatorY());
        int convertedX = (int) (joystick.getActuatorX() * BYTE_MAX);
        int convertedY = (int) (joystick.getActuatorY() * BYTE_MAX);

        Paint paint = new Paint();
        int color = getResources().getColor(R.color.green);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("Actuator value = X: " + actuatorX + ", Y: " + actuatorY, 100, 150, paint);
        canvas.drawText("Converted value = X: " + convertedX + ", Y: " + convertedY, 100, 200, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(joystick.isPressed(event.getX(), event.getY())) {
                    joystick.setIsPressed(true);
                    joystickBtWriteTask = new JoystickBtWriteTask();
                    joystickBtWriteTimer = new Timer();
                    joystickBtWriteTimer.schedule(joystickBtWriteTask, 0, BT_WRITE_INTERVAL);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if(joystick.getIsPressed()) {
                    joystick.setActuator(event.getX(), event.getY());
                }
                return true;
            case MotionEvent.ACTION_UP:
                joystick.setIsPressed(false);
                joystick.resetActuator();
                if(joystickBtWriteTimer != null) {
                    joystickBtWriteTimer.cancel();
                }
                btManager.write(new byte[]{(byte)0, (byte) 0});
                return true;
        }

        return false;
    }

    public void update() {
        joystick.update();
    }

    private class JoystickBtWriteTask extends TimerTask {

        @Override
        public void run() {
            byte xValue = (byte) (joystick.getActuatorX() * BYTE_MAX);
            byte yValue = (byte) (joystick.getActuatorY() * BYTE_MAX);
            byte[] msg = {xValue, yValue};
            btManager.write(msg);
        }
    }
}
