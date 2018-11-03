package com.emu.adam.will.physicsar;

import android.view.View;
import android.widget.TextView;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

public class Particle {

    private Node particle;
    private float charge;
    private ViewRenderable display;
    private Node arrow;
    private Vector3 force;
    private Renderable neutral;
    private Renderable neg;
    private Renderable pos;
    private int num;

    public Particle (Node particle, float charge, ViewRenderable display, Node arrow, Renderable pos, Renderable neutral, Renderable neg, int num) {
        this.particle = particle;
        this.charge = charge;
        this.display = display;
        this.arrow = arrow;
        this.pos = pos;
        this.neutral = neutral;
        this.neg = neg;
        this.num = num;
    }

    public Node getParticle() {
        return particle;
    }

    public float getCharge() {
        return charge;
    }

    public void setCharge(float charge) {
        if (this.charge * charge == 0) {
            if (charge > 0)
                particle.setRenderable(pos);
            else if (charge < 0)
                particle.setRenderable(neg);
            else
                particle.setRenderable(neutral);
        }

        this.charge = charge;
        View view = display.getView();
        TextView chargeHeader = view.findViewById(R.id.chargeHeader);
        chargeHeader.setText("" + (int)charge + " μC");

    }

    public Vector3 getForce() {
        return force;
    }

    public void setForce(Vector3 force) {
        this.force = force;

        if (force.length() > 0) {
            arrow.setEnabled(true);
            arrow.setLocalRotation(Quaternion.lookRotation(force, Vector3.cross(force, new Vector3(0, 1, 0))));
            arrow.setLocalScale(new Vector3(1, 1, 0.005f * force.length()));
        }
        else {
            arrow.setEnabled(false);
        }
    }

    public int getNum() { return num; }

}
