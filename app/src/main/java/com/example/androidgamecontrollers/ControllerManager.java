package com.example.androidgamecontrollers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/*
This manages all objects in the game and is responsible for
updating all states and render all objects to the screen
 */
class ControllerManager extends SurfaceView implements SurfaceHolder.Callback {
    private Joystick joystick;
    private AppLoop appLoop;

    public ControllerManager(Context context) {
        super(context);

        //Get surface holder and set callback
        SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        appLoop = new AppLoop(this, surfaceHolder);

        setFocusable(true);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        appLoop.startLoop();

        int joystickColor = getResources().getColor(R.color.joystickColor);
        int joystickBackground = getResources().getColor(R.color.joystickBackground);
        joystick = new Joystick(getPivotX(), getPivotY(), 150, 200, joystickColor, joystickBackground);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void draw(Canvas canvas) {
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
        String actuatorX = String.format("%.2f", joystick.getActuatorX());
        String actuatorY = String.format("%.2f", joystick.getActuatorY());

        Paint paint = new Paint();
        int color = getResources().getColor(R.color.green);
        paint.setColor(color);
        paint.setTextSize(50);
        canvas.drawText("Actuator value = X: " + actuatorX + ", Y: " + actuatorY, 100, 150, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(joystick.isPressed(event.getX(), event.getY())) {
                    joystick.setIsPressed(true);
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
                return true;
        }

        return false;
    }

    public void update() {
        joystick.update();
    }
}
