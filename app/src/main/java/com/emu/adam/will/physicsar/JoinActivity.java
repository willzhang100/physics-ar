package com.emu.adam.will.physicsar;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class JoinActivity extends AppCompatActivity {

    private FirebaseDatabase database;
    private DatabaseReference reference;
    private DatabaseReference groupReference;

    private String id;
    private String cloudAnchorID;


    private ArFragment arFragment;
    private Session session;
    private Anchor anchor;
    private AnchorNode anchorNode;
    private Renderable anchorRenderable;

    private Renderable redSphereRenderable;
    private Renderable blueSphereRenderable;
    private Renderable whiteSphereRenderable;
    private ModelRenderable arrowRenderable;
    private ViewRenderable particleControlsRenderable;
    private List<Particle> particles;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        particles = new ArrayList<>();
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        if(session == null) {
            try {
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                e.printStackTrace();
            } catch (UnavailableApkTooOldException e) {
                e.printStackTrace();
            } catch (UnavailableSdkTooOldException e) {
                e.printStackTrace();
            }

        }



        ModelRenderable.builder().setSource(this, Uri.parse("Arrow.sfb"))
                .build()
                .thenAccept(renderable -> {
                    arrowRenderable = renderable;
                })
                .exceptionally(
                        throwable -> {
                            Log.e("Fail", "Unable to load renderable", throwable);
                            return null;
                        }
                );

        ViewRenderable.builder()
                .setView(this, R.layout.particle_controls)
                .build()
                .thenAccept(renderable -> particleControlsRenderable = renderable);

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLACK))
                .thenAccept(
                        material -> {
                            anchorRenderable =
                                    ShapeFactory.makeCylinder(0.05f,.01f, new Vector3(0.0f, 0f, 0.0f), material);
                        }
                );

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            redSphereRenderable =
                                    ShapeFactory.makeSphere(0.025f, new Vector3(0.0f, 0f, 0.0f), material);
                        }
                );

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(
                        material -> {
                            blueSphereRenderable =
                                    ShapeFactory.makeSphere(0.025f, new Vector3(0.0f, 0f, 0.0f), material);
                        }
                );

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.WHITE))
                .thenAccept(
                        material -> {
                            whiteSphereRenderable =
                                    ShapeFactory.makeSphere(0.025f, new Vector3(0.0f, 0f, 0.0f), material);
                        }
                );



        database = FirebaseDatabase.getInstance();
        reference = database.getReference();

        ResolveDialogFragment dialog = new ResolveDialogFragment();
        dialog.setOkListener(this::onResolveOkPressed);
        dialog.show(getSupportFragmentManager(), "Resolve");
    }

    private void onResolveOkPressed(String dialogValue) {
        id = dialogValue;

        groupReference = reference.child(id);
        groupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("anchor")) {
                    cloudAnchorID = (String) dataSnapshot.child("anchor").getValue();
                    resolveAnchor();

//                    if (dataSnapshot.child("particles").hasChildren()) {
//                        for (DataSnapshot child : dataSnapshot.child("particles").getChildren()) {
//                            createParticle(child);
//                        }
//                    }
                } else {
                    groupReference.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                            if(dataSnapshot.hasChild("anchor")) {
                                Log.i("CHECK_DOWN", "child added of group");
                                cloudAnchorID = (String) dataSnapshot.child("anchor").getValue();
                                resolveAnchor();
                            }

//                            if (dataSnapshot.child("particles").hasChildren()) {
//                                for (DataSnapshot child : dataSnapshot.child("particles").getChildren()) {
//                                    createParticle(child);
//                                }
//                            }
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FIREBASE", "Getting name failed");
            }
        });


    }


    private void resolveAnchor() {
        Log.i("CHECK_UP", "About to resolve anchor");
        anchor = session.resolveCloudAnchor(cloudAnchorID);
        Log.i("CHECK_UP", "Resolved");
        anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        anchorNode.setRenderable(anchorRenderable);
        Log.i("CHECK_UP", "Rendered");
        //appAnchorState = AppAnchorState.RESOLVING;



        groupReference.child("particles").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //Create particle here
                Log.e("CHECK_UP", "About to create particle");
                createParticle(dataSnapshot);
                calculateNetForces(particles);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i("CHECK_DOWN", dataSnapshot.getKey());
                Log.i("CHECK_DOWN", " "+dataSnapshot.child("c").getValue().toString());
                Log.i("CHECK_DOWN", " "+dataSnapshot.child("y").getValue().toString());

                int num = Integer.valueOf(dataSnapshot.getKey());
                Log.i("CHECK_DOWN", "---"+String.valueOf(particles.size()));
                Particle p = particles.get(num);
                p.setCharge(Float.valueOf(dataSnapshot.child("c").getValue().toString()));
                Log.i("CHECK_DOWN", " charge "+String.valueOf(p.getCharge()));
                Node sphere = p.getParticle();
                Vector3 pos = sphere.getLocalPosition();
                sphere.setLocalPosition(new Vector3(pos.x, Float.valueOf(dataSnapshot.child("y").getValue().toString()), pos.z));
                calculateNetForces(particles);


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Config config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        session.configure(config);
        arFragment.getArSceneView().setupSession(session);
    }

    private void createParticle(DataSnapshot dataSnapshot) {

        Node sphere = new Node();
        sphere.setParent(anchorNode);
        sphere.setLocalPosition(new Vector3(Float.valueOf(dataSnapshot.child("x").getValue().toString()), Float.valueOf(dataSnapshot.child("y").getValue().toString()), Float.valueOf(dataSnapshot.child("z").getValue().toString())));
        sphere.setRenderable(whiteSphereRenderable);
        Log.e("CHECK_UP", "Rendered sphere");

        // Create arrow node
        Node arrow = new Node();
        arrow.setParent(sphere);
        arrow.setRenderable(arrowRenderable);
        arrow.setLocalPosition(new Vector3(0f, 0f, 0f));
        arrow.setEnabled(false);
        Log.e("CHECK_UP", "rendered arrow" + particles.size());

        // Create particle controls menu
        Node particleControls = new Node();
        particleControls.setParent(sphere);
        particleControls.setRenderable(particleControlsRenderable);
        particleControls.setLocalPosition(new Vector3(0.0f, 0.1f, 0.0f));


        particles.add(new Particle(sphere, 0f, particleControlsRenderable, arrow, redSphereRenderable, whiteSphereRenderable, blueSphereRenderable, Integer.valueOf(dataSnapshot.getKey())));
        particles.get(particles.size()-1).setCharge(Float.valueOf(dataSnapshot.child("c").getValue().toString()));


        ViewRenderable.builder()
                .setView(this, R.layout.particle_controls)
                .build()
                .thenAccept(renderable -> particleControlsRenderable = renderable);
    }



    private static void calculateNetForces(List<Particle> particles) {
        for(int i = 0; i < particles.size(); i++) {
            Vector3 netForce = new Vector3();
            Particle source = particles.get(i);
            Vector3 sourcePosition = source.getParticle().getLocalPosition();
            for(int j = 0; j < particles.size(); j++) {
                if(i != j) {
                    Particle other = particles.get(j);
                    Vector3 otherPosition = other.getParticle().getLocalPosition();
                    Vector3 direction = Vector3.subtract(sourcePosition, otherPosition);
                    float r = direction.length();
                    direction.normalized();
                    Vector3 force = direction.scaled((source.getCharge() * other.getCharge()) / (r * r));
                    netForce = Vector3.add(netForce, force);
                }
            }
            source.setForce(netForce);
        }
    }


}
