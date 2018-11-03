package com.emu.adam.will.physicsar;

import android.net.Uri;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HostActivity extends AppCompatActivity {

    private Renderable redSphereRenderable;
    private Renderable blueSphereRenderable;
    private Renderable whiteSphereRenderable;
    private Renderable anchorRenderable;
    private ModelRenderable arrowRenderable;
    private ViewRenderable particleControlsRenderable;
    private ArFragment arFragment;
    private Anchor anchor;
    private AnchorNode anchorNode;
    private float yCoord = -1;
    private float xCoord = -1;
    private List<Particle> particles;
    private Session session;
    private Map<Node, Particle> map;

    private FirebaseDatabase database;
    private DatabaseReference reference;
    private DatabaseReference groupReference;

    private int num;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED
    }

    @GuardedBy("singleTapAnchorLock")
    private AppAnchorState appAnchorState = AppAnchorState.NONE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        map = new HashMap<>();

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        groupReference = reference;


        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                groupReference = reference.child(String.valueOf(dataSnapshot.getChildrenCount()));
                groupReference.setValue("Created");
                groupReference.child("particles").setValue("Created");

                Log.i("FIREBASE", "hey it worked");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FIREBASE", "Getting name failed");
            }
        });

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

        /**
         CompletableFuture<ViewRenderable> particleControlsStage =
         ViewRenderable.builder().setView(this, R.layout.particle_controls).build();

         CompletableFuture.allOf(particleControlsStage)
         .handle(
         (notUsed, throwable) -> {
         if (throwable != null) {
         //Unable to load renderables
         return null;
         }
         try {
         particleControlsRenderable = particleControlsStage.get();

         // Everything finished loading successfully.
         hasFinishedLoading = true;
         } catch (InterruptedException | ExecutionException ex) {
         //Unable to load renderable
         }
         return null;
         });

         **/

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (arFragment == null || appAnchorState == AppAnchorState.HOSTING) {
                        return;
                    }

                    if (anchor == null) {
                        // Create the anchor once
                        //anchor = hitResult.createAnchor();
                        Log.d("anchor", "Before the anchor is created");
                        anchor = hitResult.createAnchor();
                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        anchorNode.setRenderable(anchorRenderable);
                        if(anchor.getTrackingState() == TrackingState.TRACKING) {
                            anchor = session.hostCloudAnchor(anchor);
                            Log.d("anchor", "After the anchor is created");
                            appAnchorState = appAnchorState.HOSTING;
                            Snackbar.make(findViewById(R.id.constraint_layout),
                                    "Hosting cloud anchor ...", Snackbar.LENGTH_LONG)
                                    .show();
                        }
                        return;
                    }

                    //TODO before reset button
                    if (anchor.getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }

                    float[] anchorPoint = anchor.getPose().getTranslation();
                    float[] hitPoint = hitResult.getHitPose().getTranslation();

                    Renderable sphereRenderable = whiteSphereRenderable;

                    // Create sphere node
                    Node sphere = new Node();
                    sphere.setParent(anchorNode);
                    sphere.setLocalPosition(new Vector3(hitPoint[0] - anchorPoint[0], 0.05f, hitPoint[2] - anchorPoint[2]));
                    sphere.setRenderable(sphereRenderable);

                    // Create arrow node
                    Node arrow = new Node();
                    arrow.setParent(sphere);
                    arrow.setRenderable(arrowRenderable);
                    arrow.setLocalPosition(new Vector3(0f, 0f, 0f));
                    arrow.setEnabled(false);


                    particles.add(new Particle(sphere, 0f, particleControlsRenderable, arrow, redSphereRenderable, whiteSphereRenderable, blueSphereRenderable, num));
                    map.put(sphere, particles.get(particles.size()-1));
                    DatabaseReference newParticle = groupReference.child("particles").child(String.valueOf(num));
                    newParticle.child("c").setValue("0");
                    Vector3 tempPos = sphere.getLocalPosition();
                    newParticle.child("x").setValue(tempPos.x);
                    newParticle.child("y").setValue(tempPos.y);
                    newParticle.child("z").setValue(tempPos.z);
                    num++;




                    // Create particle controls menu
                    Node particleControls = new Node();
                    particleControls.setParent(sphere);
                    particleControls.setRenderable(particleControlsRenderable);
                    particleControls.setLocalPosition(new Vector3(0.0f, 0.1f, 0.0f));


                    sphere.setOnTouchListener(
                            ((hitTestResult, motionEvent1) -> {
                                if (motionEvent1.getAction() == MotionEvent.ACTION_DOWN) {
                                    yCoord = motionEvent1.getRawY();
                                    xCoord = motionEvent1.getRawX();
                                    return true;
                                }

                                if (motionEvent1.getAction() == MotionEvent.ACTION_UP) {
                                    Particle particle = map.get(sphere);
                                    if (yCoord != -1) {
                                        float differenceY = motionEvent1.getRawY() - yCoord;
                                        if (differenceY > 100 && sphere.getLocalPosition().y >= .05f) {
                                            Vector3 pos = sphere.getLocalPosition();
                                            sphere.setLocalPosition(new Vector3(pos.x, pos.y - .05f, pos.z));
                                            calculateNetForces(particles);
                                            groupReference.child("particles").child(String.valueOf(particle.getNum())).child("y").setValue(String.valueOf(pos.y - .05f));
                                            return true;
                                        }
                                        if (differenceY < -100) {
                                            Vector3 pos = sphere.getLocalPosition();
                                            sphere.setLocalPosition(new Vector3(pos.x, pos.y + .05f, pos.z));
                                            calculateNetForces(particles);
                                            groupReference.child("particles").child(String.valueOf(particle.getNum())).child("y").setValue(String.valueOf(pos.y + .05f));
                                            return true;
                                        }
                                    }
                                    if (xCoord != -1 && Math.abs(motionEvent1.getRawX() - xCoord) > 100) {
                                        float differenceX = motionEvent1.getRawX() - xCoord;
                                        Particle p = null;
                                        for (Particle p1 : particles) {
                                            if (p1.getParticle() == sphere)
                                                p = p1;
                                        }

                                        if (differenceX < -100) {
                                            p.setCharge(p.getCharge() - 5);
                                            calculateNetForces(particles);
                                            groupReference.child("particles").child(String.valueOf(particle.getNum())).child("c").setValue(String.valueOf(p.getCharge()));

                                            return true;
                                        }

                                        if (differenceX > 100) {
                                            p.setCharge(p.getCharge() + 5);
                                            calculateNetForces(particles);

                                            groupReference.child("particles").child(String.valueOf(particle.getNum())).child("c").setValue(String.valueOf(p.getCharge()));

                                            return true;
                                        }
                                    }
                                    particleControls.setEnabled(!particleControls.isEnabled());
                                }
                                return false;
                            })
                    );


                    Log.i("distance", "" + calculateDistance(sphere.getLocalPosition(), new Vector3(0f, 0f, 0f)));


                    //Prepares render for another particle's controls
                    ViewRenderable.builder()
                            .setView(this, R.layout.particle_controls)
                            .build()
                            .thenAccept(renderable -> particleControlsRenderable = renderable);
                }

        );


        arFragment.getArSceneView().getScene().addOnUpdateListener(
                frameTime -> {
                    if(appAnchorState == AppAnchorState.HOSTING) {
                        Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
                        if(cloudState.isError()) {
                            Snackbar.make(findViewById(R.id.constraint_layout),
                                    "Error hosting anchor: " + cloudState,
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            appAnchorState = AppAnchorState.NONE;
                        } else if(cloudState == Anchor.CloudAnchorState.SUCCESS) {
                            Snackbar.make(findViewById(R.id.constraint_layout),
                                    "Anchor successfully hosted! Cloud ID: " + anchor.getCloudAnchorId(),
                                    Snackbar.LENGTH_LONG)
                                    .show();
                            appAnchorState = AppAnchorState.HOSTED;
                            groupReference.child("anchor").setValue(anchor.getCloudAnchorId());
                            //sendPost();
                            //update();
                        }
                    }
                }
        );
    }

    private static float calculateDistance(Vector3 a, Vector3 b) {

        // Compute difference vector between the two hit locations
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;

        // Compute the euclidean distance
        return (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
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

    @Override
    protected void onResume() {
        super.onResume();
        Config config = new Config(session);
        config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
        session.configure(config);
        arFragment.getArSceneView().setupSession(session);
    }

    public static void setValue(DatabaseReference reference, String value) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(30);
                    //sendPost();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
