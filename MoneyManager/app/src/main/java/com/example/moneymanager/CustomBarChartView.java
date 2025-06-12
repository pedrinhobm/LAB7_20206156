package com.example.moneymanager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CustomBarChartView extends View { // este es el grafico de barras
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

    private void init() { // aqui usa IA para distribuir el tamaño textos y etiquetas
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(double ingresos, double egresos) {
        this.ingresosValue = (float) ingresos;
        this.egresosValue = (float) egresos;
        this.consolidadoValue = (float) (ingresos - egresos); // de igual manera usamos el consolidado para restar los ingresos y egresos

        maxValue = Math.max(ingresosValue, egresosValue); // con Math calculaoms los valores maximos para escalar las barras
        maxValue = Math.max(maxValue, Math.abs(consolidadoValue)); // si salió negativo, lo asignamos como valor absoluto
        if (maxValue == 0) {
            maxValue = 100;
        }
        invalidate(); // tomando en cuento el anterior grfico ,lo redibujamos
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth(); // de igual manera al anterior grafico
        int height = getHeight(); // use IA para medir los espacios y tamaños de las barras a distribuir
        float barWidth = width / 6f; // Un sexto del ancho total para cada barra + espacios
        float spacing = barWidth / 2f;
        float bottomPadding = 50f;
        float topPadding = 50f; // estw ultimo fue para ajustar con el borde que lo limita
        float chartHeight = height - bottomPadding - topPadding;

        barPaint.setColor(getResources().getColor(android.R.color.holo_green_dark)); // barra de ingresos
        float ingresosBarHeight = (ingresosValue / maxValue) * chartHeight; // cada uno medimos sobre el máximo valor por la altura
        canvas.drawRect(spacing, height - bottomPadding - ingresosBarHeight, spacing + barWidth, height - bottomPadding, barPaint);
        canvas.drawText("Ingresos", spacing + barWidth / 2, height - bottomPadding + 30, textPaint);

        barPaint.setColor(getResources().getColor(android.R.color.holo_red_dark)); // barra de egresos
        float egresosBarHeight = (egresosValue / maxValue) * chartHeight;  // en cada uno medimos por cada lado izq y derecho
        canvas.drawRect(spacing * 2 + barWidth, height - bottomPadding - egresosBarHeight, spacing * 2 + barWidth * 2, height - bottomPadding, barPaint);
        canvas.drawText("Egresos", spacing * 2 + barWidth * 1.5f, height - bottomPadding + 30, textPaint);

        if (consolidadoValue >= 0) { // ahora dibujamos la barra consolidada
            barPaint.setColor(getResources().getColor(android.R.color.holo_blue_dark)); // si es positivo, será de color azul
        } else {
            barPaint.setColor(getResources().getColor(android.R.color.holo_orange_dark)); // pero de ser negativo, será anaranjado
        }
        float consolidadoBarHeight = (Math.abs(consolidadoValue) / maxValue) * chartHeight; // lo mismo realizo para medir las longitudes como las barras anteriores de ingresos y egresos
        canvas.drawRect(spacing * 3 + barWidth * 2, height - bottomPadding - consolidadoBarHeight, spacing * 3 + barWidth * 3, height - bottomPadding, barPaint);
        canvas.drawText("Consolidado", spacing * 3 + barWidth * 2.5f, height - bottomPadding + 30, textPaint);
    }
}