package com.example.moneymanager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CustomPieChartView extends View { // este es el gráfico del pastel de porcentaje
    private Paint slicePaint;
    private RectF oval;
    private List<PieSlice> slices;

    public CustomPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() { // aqui se va distribuir las porciones del pastel
        slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG); // a traves de un arreglo y color
        slicePaint.setStyle(Paint.Style.FILL);
        slices = new ArrayList<>();
    }

    public void setData(float ingresos, float egresos) {
        slices.clear();
        float total = ingresos + egresos; // para ello sumamos ambos parametros

        if (total == 0) {
            invalidate(); // en caso no haya nada , el invalidate lo asigna como vacio
            return;
        }

        float ingresosAngle = (ingresos / total) * 360f; // estos calculos haran para calcular los angulos
        float egresosAngle = (egresos / total) * 360f; // de cada porción

        if (ingresos > 0) { // añadimos los segmentos para cada porción de ingreso y egreso
            slices.add(new PieSlice(ingresosAngle, getResources().getColor(android.R.color.holo_green_dark)));
        }
        if (egresos > 0) { // en cada uno lo diferenciamos por colores
            slices.add(new PieSlice(egresosAngle, getResources().getColor(android.R.color.holo_red_dark)));
        }
        invalidate(); // si hay ingresos y egresos, valida que se coloree las porciones
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty()) { // en una porcion vacia solo dibujamos un círculo gris
            slicePaint.setColor(Color.LTGRAY); // aqui si usa IA para obtener el ancho, altura y tamaño del círculo
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4f, slicePaint);
            slicePaint.setColor(Color.DKGRAY);
            slicePaint.setTextSize(30f);
            slicePaint.setTextAlign(Paint.Align.CENTER); // así como el mensaje que indica que no hay datos
            canvas.drawText("No hay datos", getWidth() / 2f, getHeight() / 2f, slicePaint);
            return;
        }

        if (oval == null) {
            float size = Math.min(getWidth(), getHeight()) * 0.8f; //igual aqui con la función Math
            float left = (getWidth() - size) / 2f; // para medir el tamaño de la figura con los datos del ancho y altura
            float top = (getHeight() - size) / 2f;
            oval = new RectF(left, top, left + size, top + size);
        }

        float startAngle = 0;
        for (PieSlice slice : slices) {
            slicePaint.setColor(slice.color);
            canvas.drawArc(oval, startAngle, slice.angle, true, slicePaint);
            startAngle += slice.angle; // por cada slice medimos la sumatoria de los angulos para dibujar las porciones de ingresos y egresos
        }
    }

    private static class PieSlice { // esta es la porcion , que mide el color y ángulo
        float angle;
        int color;
        PieSlice(float angle, int color) {
            this.angle = angle;
            this.color = color;
        }
    }
}