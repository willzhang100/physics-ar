package com.emu.adam.will.physicsar;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

import java.util.HashMap;
import java.util.Map;

public class Arrow {

    public Node arrow;
    private Map<Particle, Vector3> forces;
    private Vector3 netForce;

    public Arrow(Node arrow) {
        this.arrow = arrow;
        forces = new HashMap<>();
        netForce = new Vector3(0,0,0);
    }

    //When new particle added or a particle is moved
    public void updatePosition(Vector3 newPos, Particle particle) {
        //particle represents the particle that was changed to cause this
        arrow.setLocalPosition(newPos);

        if (!forces.containsKey(particle)) {
            netForce = Vector3.add(netForce,calculateForce(particle));
        }

        for (Particle p : forces.keySet())
            updateDirection(p);
    }

    //When particle charge is changed
    public void updateDirection(Particle p) {
        //Change transparency in here depending on force strength
        Vector3 newNetForce = new Vector3(netForce);
        newNetForce = Vector3.subtract(newNetForce, forces.get(p));
        netForce = Vector3.add(newNetForce, calculateForce(p));


        arrow.setLocalRotation(Quaternion.lookRotation(netForce, Vector3.cross(netForce, new Vector3(0, 1, 0))));
        arrow.setLocalScale(new Vector3(.4f, .4f, .25f));   //0.005f * netForce.length()
        if (netForce.length() < .001f) {
            arrow.setEnabled(false);
        }
        else
            arrow.setEnabled(true);
        //TODO transparency?
    }

    private Vector3 calculateForce(Particle p) {
        Vector3 force;

        Vector3 otherPosition = p.getParticle().getLocalPosition();
        Vector3 direction = Vector3.subtract(arrow.getLocalPosition(), otherPosition);
        float r = direction.length();       //Strength of force
        direction.normalized();
        force = direction.scaled((p.getCharge()) / (r * r));

        forces.put(p, force);
        return force;
    }

}
