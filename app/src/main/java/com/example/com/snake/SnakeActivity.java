package com.example.com.snake;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

public class SnakeActivity extends Activity {

    // Declara una instancia de SnakeView
    SnakeView snakeView;
    // Lo cargaremos en onCreate
    // Una vez que tengamos más detalles sobre
    // el dispositivo del jugador
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // averiguar el ancho y alto de la pantalla
        Display display = getWindowManager().getDefaultDisplay();

        // Cargar la resolución en un objeto Punto
        Point size = new Point();
        display.getSize(size);

        // Crea una nueva vista basada en la clase SnakeView
        snakeView = new SnakeView(this, size);

        // Clase SnakeView la vista por defecto de nuestra actividad
        setContentView(snakeView);
    }

    // Empezar el hilo en SnakeView cuando esta Activity
    // sea mostrada al jugador
    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    // Asegurarse de que el hilo en SnakeView sea detenido
    // Si esta Activity está por ser cerrada.
    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }
}

