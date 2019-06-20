package com.example.com.snake;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

class SnakeView extends SurfaceView implements Runnable {

    // Todo el código correrá separado a la UI
    private Thread m_Thread = null;
    // Esta variable determina cuando el juego está corriendo
    // Es declarada como volátil porque
    // puede ser accesada desde dentro y fuera del hilo
    private volatile boolean m_Playing;

    // Esta es sobre la que trazamos
    private Canvas m_Canvas;
    // Esta es requerida por la clase Canvas para hacer el trazo
    private SurfaceHolder m_Holder;
    // Esta nos deja controlar colores etc
    private Paint m_Paint;

    // Esta será una referencia a la Activity
    private Context m_context;

    // Sonido
    private SoundPool m_SoundPool;
    private int m_get_mouse_sound = -1;
    private int m_dead_sound = -1;

    // Para seguir movimiento m_Direction
    public enum Direction {UP, RIGHT, DOWN, LEFT}
    // Inicia dirigiéndose a la derecha
    private Direction m_Direction = Direction.RIGHT;

    // Cual es la resolución de la pantalla
    private int m_ScreenWidth;
    private int m_ScreenHeight;

    // Controla la pausa entre actualizaciones
    private long m_NextFrameTime;
    // Actualiza el juego 10 veces por segundo
    private final long FPS = 10;
    // Hay 1000 milisegundos en un segundo
    private final long MILLIS_IN_A_SECOND = 1000;
    // Trazaremos el marco mucho más seguido

    // El m_Score actual
    private int m_Score;

    // La locación en la cuadrícula de todos los segmentos
    private int[] m_SnakeXs;
    private int[] m_SnakeYs;

    // Cuán larga es la serpiente en ese momento
    private int m_SnakeLength;

    // Donde está el ratón
    private int m_MouseX;
    private int m_MouseY;

    // El tamaño en pixeles de un segmento de serpiente
    private int m_BlockSize;

    // El tamaño en segmentos del área de juego
    private final int NUM_BLOCKS_WIDE = 40;
    private int m_NumBlocksHigh; // determinado dinámicamente

    public SnakeView(Context context, Point size) {
        super(context);

        m_context = context;

        m_ScreenWidth = size.x;
        m_ScreenHeight = size.y;

        //Determinar el tamaño de cada bloque/lugar en el tablero de juego
        m_BlockSize = m_ScreenWidth / NUM_BLOCKS_WIDE;
        // Cuantos bloques del mismo tamaño encajarán en la altura
        m_NumBlocksHigh = ((m_ScreenHeight)) / m_BlockSize;

        // Establece el sonido
        loadSound();

        // Inicializa los objetos de trazado
        m_Holder = getHolder();
        m_Paint = new Paint();

        // Si haces 200 puntos eres recompensando con un logro de crash!
        m_SnakeXs = new int[200];
        m_SnakeYs = new int[200];

        // Empezar el juego
        startGame();
    }

    @Override
    public void run() {
        // El chequeo de m_Playing previene un crash al inicio
        // Podrías además extender el código
        // para proveer una característica de pausa
        while (m_Playing) {

            // Actualiza 10 veces por segundo
            if(checkForUpdate()) {
                updateGame();
                drawGame();
            }

        }
    }
    //Carga sonido
    public void loadSound() {
        m_SoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {

            // Crear objetos de las 2 clases requeridas.
            // Usa m_Context porque esta es una referencia a la Actividad
            AssetManager assetManager = m_context.getAssets();
            AssetFileDescriptor descriptor;

            // Preparar los dos sonidos en la memoria.
            descriptor = assetManager.openFd("get_mouse_sound.ogg");
            m_get_mouse_sound = m_SoundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("death_sound.ogg");
            m_dead_sound = m_SoundPool.load(descriptor, 0);


        } catch (IOException e) {
            // Error
        }
    }

    public void startGame() {
        // Empieza con solo una cabeza, en el centro de la pantalla
        m_SnakeLength = 20;
        m_SnakeXs[0] = NUM_BLOCKS_WIDE / 2;
        m_SnakeYs[0] = m_NumBlocksHigh / 2;

        // Y un ratón para comer
        spawnMouse();

        // Resetea el m_Score
        m_Score = 0;

        // Establece m_NextFrameTime para que una actualización
        // sea provocada inmediatamente
        m_NextFrameTime = System.currentTimeMillis();
    }

    public void spawnMouse() {
        Random random = new Random();
        m_MouseX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        m_MouseY = random.nextInt(m_NumBlocksHigh - 1) + 1;
    }
    //Se come al raton
    private void eatMouse(){
        //  Me comí uno! Chilla!!
        // Incrementa el largo de la serpiente
        m_SnakeLength++;
        //Reemplaza el ratón
        spawnMouse();
        //Añade al m_Score
        m_Score = m_Score + 1;
        m_SoundPool.play(m_get_mouse_sound, 1, 1, 0, 0, 1);
    }

    private void moveSnake(){
        // Mueve el cuerpo
        for (int i = m_SnakeLength; i > 0; i--) {
            // Empieza atrás y muévelo
            // a la posición del segmento delante de el
            m_SnakeXs[i] = m_SnakeXs[i - 1];
            m_SnakeYs[i] = m_SnakeYs[i - 1];

            // Excluye la cabeza porque
            // la cabeza no tiene nada delante de ella
        }

        // Mueve la cabeza en la m_Direction apropiada
        switch (m_Direction) {
            case UP:
                m_SnakeYs[0]--;
                break;

            case RIGHT:
                m_SnakeXs[0]++;
                break;

            case DOWN:
                m_SnakeYs[0]++;
                break;

            case LEFT:
                m_SnakeXs[0]--;
                break;
        }
    }

    private boolean detectDeath(){
        // ¿Ha muerto la serpiente?
        boolean dead = false;

        // ¿Golpea una pared?
        if (m_SnakeXs[0] == -1) dead = true;
        if (m_SnakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
        if (m_SnakeYs[0] == -1) dead = true;
        if (m_SnakeYs[0] == m_NumBlocksHigh) dead = true;

        // ¿Comido a sí misma?
        for (int i = m_SnakeLength - 1; i > 0; i--) {
            if ((i > 4) && (m_SnakeXs[0] == m_SnakeXs[i]) && (m_SnakeYs[0] == m_SnakeYs[i])) {
                dead = true;
            }
        }

        return dead;
    }
    //Actualiza el juego
    public void updateGame() {
        // ¿La cabeza de la serpiente tocó al ratón?
        if (m_SnakeXs[0] == m_MouseX && m_SnakeYs[0] == m_MouseY) {
            eatMouse();
        }

        moveSnake();

        if (detectDeath()) {
            //Empezar de nuevo
            m_SoundPool.play(m_dead_sound, 1, 1, 0, 0, 1);

            startGame();
        }
    }
    //Dibuja el Juego
    public void drawGame() {
        // Prepara para trazar
        if (m_Holder.getSurface().isValid()) {
            m_Canvas = m_Holder.lockCanvas();

            // Despeja la pantalla con mi color favorito
            m_Canvas.drawColor(Color.argb(255, 120, 197, 87));

            // Establece el color de la pintura para trazar a la serpiente y al ratón
            m_Paint.setColor(Color.argb(255, 255, 255, 255));

            // Elige cuán grande será el score
            m_Paint.setTextSize(30);
            m_Canvas.drawText("Score:" + m_Score, 10, 30, m_Paint);


            //Traza a la serpiente
            for (int i = 0; i < m_SnakeLength; i++) {
                m_Canvas.drawRect(m_SnakeXs[i] * m_BlockSize,
                        (m_SnakeYs[i] * m_BlockSize),
                        (m_SnakeXs[i] * m_BlockSize) + m_BlockSize,
                        (m_SnakeYs[i] * m_BlockSize) + m_BlockSize,
                        m_Paint);
            }

            //Traza a el ratón
            m_Canvas.drawRect(m_MouseX * m_BlockSize,
                    (m_MouseY * m_BlockSize),
                    (m_MouseX * m_BlockSize) + m_BlockSize,
                    (m_MouseY * m_BlockSize) + m_BlockSize,
                    m_Paint);

            // Traza todo el cuadro
            m_Holder.unlockCanvasAndPost(m_Canvas);
        }

    }

    public boolean checkForUpdate() {

        // Es momento de actualizar el cuadro
        if(m_NextFrameTime <= System.currentTimeMillis()){
            // Una décima de segundo ha pasado

            // Establece cuando la próxima actualización será disparada
            m_NextFrameTime =System.currentTimeMillis() + MILLIS_IN_A_SECOND / FPS;

            // Devuelve verdadero para que la actualización y las funciones
            // de trazado sean ejecutadas
            return true;
        }

        return false;
    }


    public void pause() {
        m_Playing = false;
        try {
            m_Thread.join();
        } catch (InterruptedException e) {
            // Error
        }
    }

    public void resume() {
        m_Playing = true;
        m_Thread = new Thread(this);
        m_Thread.start();
    }
    //Movimiento Snake en la pantalla
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= m_ScreenWidth / 2) {
                    switch(m_Direction){
                        case UP: //Esta arriba
                            m_Direction = Direction.RIGHT;//Derecha
                            break;
                        case RIGHT://Esta derecha
                            m_Direction = Direction.DOWN;//Abajo
                            break;
                        case DOWN://Abajo
                            m_Direction = Direction.LEFT;//Izquierda
                            break;
                        case LEFT://Izquierda
                            m_Direction = Direction.UP;//Arriba
                            break;
                    }
                } else {
                    switch(m_Direction){
                        case UP://Esta arriba
                            m_Direction = Direction.LEFT;//Izquierda
                            break;
                        case LEFT://Esta Izquierda
                            m_Direction = Direction.DOWN;//Abajo
                            break;
                        case DOWN://Esta Abajo
                            m_Direction = Direction.RIGHT;//Derecha
                            break;
                        case RIGHT://Esta Derecha
                            m_Direction = Direction.UP;//Arriba
                            break;
                    }
                }
        }
        return true;
    }
}
