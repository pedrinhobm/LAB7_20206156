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

public class CustomPieChartView extends View {

    private Paint slicePaint;
    private RectF oval;
    private List<PieSlice> slices;

    public CustomPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        slicePaint.setStyle(Paint.Style.FILL);
        slices = new ArrayList<>();
    }

    public void setData(float ingresos, float egresos) {
        slices.clear();
        float total = ingresos + egresos;

        if (total == 0) {
            // No hay datos, el gráfico estará vacío
            invalidate(); // Redibujar
            return;
        }

        // Calcular ángulos
        float ingresosAngle = (ingresos / total) * 360f;
        float egresosAngle = (egresos / total) * 360f;

        // Añadir los segmentos
        if (ingresos > 0) {
            slices.add(new PieSlice(ingresosAngle, getResources().getColor(android.R.color.holo_green_dark)));
        }
        if (egresos > 0) {
            slices.add(new PieSlice(egresosAngle, getResources().getColor(android.R.color.holo_red_dark)));
        }

        invalidate(); // Redibujar el gráfico
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty()) {
            // Dibujar un mensaje o un círculo gris si no hay datos
            slicePaint.setColor(Color.LTGRAY);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4f, slicePaint);
            slicePaint.setColor(Color.DKGRAY);
            slicePaint.setTextSize(30f);
            slicePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No hay datos", getWidth() / 2f, getHeight() / 2f, slicePaint);
            return;
        }

        if (oval == null) {
            float size = Math.min(getWidth(), getHeight()) * 0.8f; // Usar el 80% del menor lado
            float left = (getWidth() - size) / 2f;
            float top = (getHeight() - size) / 2f;
            oval = new RectF(left, top, left + size, top + size);
        }

        float startAngle = 0;
        for (PieSlice slice : slices) {
            slicePaint.setColor(slice.color);
            canvas.drawArc(oval, startAngle, slice.angle, true, slicePaint);
            startAngle += slice.angle;
        }
    }

    private static class PieSlice {
        float angle;
        int color;

        PieSlice(float angle, int color) {
            this.angle = angle;
            this.color = color;
        }
    }
}