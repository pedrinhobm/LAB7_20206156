package com.example.moneymanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CustomBarChartView extends View {

    private Paint barPaint;
    private Paint textPaint;
    private float ingresosValue = 0;
    private float egresosValue = 0;
    private float consolidadoValue = 0;
    private float maxValue = 0;

    public CustomBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f); // Tamaño del texto para las etiquetas
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(double ingresos, double egresos) {
        this.ingresosValue = (float) ingresos;
        this.egresosValue = (float) egresos;
        this.consolidadoValue = (float) (ingresos - egresos);

        // Calcular el valor máximo para escalar las barras
        maxValue = Math.max(ingresosValue, egresosValue);
        maxValue = Math.max(maxValue, Math.abs(consolidadoValue)); // Considerar el valor absoluto para consolidado
        if (maxValue == 0) maxValue = 100; // Evitar división por cero o barras invisibles si todo es cero

        invalidate(); // Redibujar las barras
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Margen y ancho de barra
        float barWidth = width / 6f; // Un sexto del ancho total para cada barra + espacios
        float spacing = barWidth / 2f; // Espacio entre barras
        float bottomPadding = 50f; // Espacio para etiquetas de texto
        float topPadding = 50f; // Espacio superior para que las barras no toquen el borde

        float chartHeight = height - bottomPadding - topPadding;

        // Dibujar barra de Ingresos
        barPaint.setColor(getResources().getColor(android.R.color.holo_green_dark));
        float ingresosBarHeight = (ingresosValue / maxValue) * chartHeight;
        canvas.drawRect(spacing, height - bottomPadding - ingresosBarHeight, spacing + barWidth, height - bottomPadding, barPaint);
        canvas.drawText("Ingresos", spacing + barWidth / 2, height - bottomPadding + 30, textPaint);


        // Dibujar barra de Egresos
        barPaint.setColor(getResources().getColor(android.R.color.holo_red_dark));
        float egresosBarHeight = (egresosValue / maxValue) * chartHeight;
        canvas.drawRect(spacing * 2 + barWidth, height - bottomPadding - egresosBarHeight, spacing * 2 + barWidth * 2, height - bottomPadding, barPaint);
        canvas.drawText("Egresos", spacing * 2 + barWidth * 1.5f, height - bottomPadding + 30, textPaint);


        // Dibujar barra de Consolidado
        if (consolidadoValue >= 0) {
            barPaint.setColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            barPaint.setColor(getResources().getColor(android.R.color.holo_orange_dark)); // Naranja para consolidado negativo
        }
        float consolidadoBarHeight = (Math.abs(consolidadoValue) / maxValue) * chartHeight;
        canvas.drawRect(spacing * 3 + barWidth * 2, height - bottomPadding - consolidadoBarHeight, spacing * 3 + barWidth * 3, height - bottomPadding, barPaint);
        canvas.drawText("Consolidado", spacing * 3 + barWidth * 2.5f, height - bottomPadding + 30, textPaint);

        // Puedes añadir un eje Y simple o etiquetas de valor si lo deseas, pero esto es lo básico
    }
}