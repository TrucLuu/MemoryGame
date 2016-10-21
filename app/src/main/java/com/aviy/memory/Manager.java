package com.aviy.memory;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.adways.planetarcade.sdk.OnScoreCallback;
import jp.co.adways.planetarcade.sdk.OnValidateCallback;
import jp.co.adways.planetarcade.sdk.PlanetArcadeSDKUtil;
import jp.co.adways.planetarcade.sdk.modules.PlanetArcadeSDKHelper;

public class Manager extends Activity {

    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private static final String TAG = "MemoryGame";
    public static final String MyPREFERENCES = "MyPrefs";
    public static final String ClientIDKEY = "ClientIDKEY";
    public boolean isOpenFromPlanetArcade;

    private static int ROW_COUNT = -1;
    private static int COL_COUNT = -1;
    private Context context;
    private Drawable backImage;
    private int[][] cards;
    private List<Drawable> images;
    private Card firstCard;
    private Card seconedCard;
    private ButtonListener buttonListener;
    private Button button;

    private static Object lock = new Object();
    int score = 0;
    int turns;
    private TableLayout mainTable;
    private UpdateCardsHandler handler;
    private TextView tvScore;
    private SharedPreferences sharedPreferences;
    private String clientId;
    private PlanetArcadeSDKHelper mPlanetArcadeSDKHelper;
    private PlanetArcadeSDKUtil mPlanetArcadeSDKUtil;
    private boolean isFromPA;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intend = getIntent();
        if (intend != null) {
            boolean isOpenedFromPA = intend.getBooleanExtra("planetarcade", false);
            if (isOpenedFromPA) {
                isFromPA = true;
            } else {
                isFromPA = false;
            }
        }
        mPlanetArcadeSDKHelper = new PlanetArcadeSDKHelper(this, true);
//        mPlanetArcadeSDKUtil = new PlanetArcadeSDKUtil();
        mPlanetArcadeSDKHelper.validateRemote(new OnValidateCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i, String s) {

            }
        });
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            mPlanetArcadeSDKHelper.getHashKey(getApplicationContext());
            Log.d("KEY HASH", "TEST   " + mPlanetArcadeSDKHelper.getHashKey(getApplicationContext()));

            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.e("name not found", e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e("no such an algorithm", e.toString());
        }
        if (Build.VERSION.SDK_INT >= 23) {
            boolean isAllDone = getListPermission(this);
            if (isAllDone) {
                startGame();
            }
        } else {
            startGame();
        }

    }

    private void startGame() {
        sharedPreferences = getSharedPreferences(MyPREFERENCES, MODE_PRIVATE);
        clientId = getClientId();
        if (TextUtils.isEmpty(clientId)) {
            clientId = createClientId();
        }
        Log.i(TAG, "clientID: " + clientId);
        mPlanetArcadeSDKHelper.setDebugMode(true);

        handler = new UpdateCardsHandler();
        loadImages();
        setContentView(R.layout.main);
        button = (Button) findViewById(R.id.btn_switch_to_app);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                openApp();
            }
        });

        tvScore = (TextView) findViewById(R.id.score);
        tvScore.setText("0");
        tvScore.setVisibility(View.INVISIBLE);

        backImage = getResources().getDrawable(R.drawable.icon);


        buttonListener = new ButtonListener();

        mainTable = (TableLayout) findViewById(R.id.TableLayout03);


        context = mainTable.getContext();

        Spinner s = (Spinner) findViewById(R.id.Spinner01);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(
                this, R.array.type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        if (isFromPA == true) {
            newGame(2, 2);
        } else if (isFromPA == false) {
            newGame(6, 6);
        } else {
            s.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(
                        android.widget.AdapterView<?> arg0,
                        View arg1, int pos, long arg3) {

                    ((Spinner) findViewById(R.id.Spinner01)).setSelection(0);

                    int x, y;

                    switch (pos) {
                        case 1:
                            x = 2;
                            y = 2;
                            break;
                        case 2:
                            x = 4;
                            y = 5;
                            break;
                        case 3:
                            x = 4;
                            y = 6;
                            break;
                        case 4:
                            x = 5;
                            y = 6;
                            break;
                        case 5:
                            x = 6;
                            y = 6;
                            break;
                        default:
                            return;
                    }
                    newGame(x, y);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub

                }

            });
        }
    }

    private String createClientId() {
        String address = "";
        if (Build.VERSION.SDK_INT >= 23) {
            address = getMACAddress("wlan0");
        } else {
            WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = manager.getConnectionInfo();
            address = info.getMacAddress();
        }
        Date date = new Date();
        String clientId = address + date.getTime();
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(ClientIDKEY, clientId);
        editor.commit();
        return clientId;

    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    private String getClientId() {
        return sharedPreferences.getString(ClientIDKEY, "");
    }

    private void newGame(int c, int r) {
        ROW_COUNT = r;
        COL_COUNT = c;
        score = 0;
        cards = new int[COL_COUNT][ROW_COUNT];


        mainTable.removeView(findViewById(R.id.TableRow01));
        mainTable.removeView(findViewById(R.id.TableRow02));

        TableRow tr = ((TableRow) findViewById(R.id.TableRow03));
        tr.removeAllViews();

        mainTable = new TableLayout(context);
        tr.addView(mainTable);

        for (int y = 0; y < ROW_COUNT; y++) {
            mainTable.addView(createRow(y));
        }

        firstCard = null;
        loadCards();

        turns = 0;
        ((TextView) findViewById(R.id.tv1)).setText("Tries: " + turns);
        tvScore.setText("0");
        tvScore.setVisibility(View.INVISIBLE);

    }

    private void loadImages() {
        images = new ArrayList<Drawable>();

        images.add(getResources().getDrawable(R.drawable.card1));
        images.add(getResources().getDrawable(R.drawable.card2));
        images.add(getResources().getDrawable(R.drawable.card3));
        images.add(getResources().getDrawable(R.drawable.card4));
        images.add(getResources().getDrawable(R.drawable.card5));
        images.add(getResources().getDrawable(R.drawable.card6));
        images.add(getResources().getDrawable(R.drawable.card7));
        images.add(getResources().getDrawable(R.drawable.card8));
        images.add(getResources().getDrawable(R.drawable.card9));
        images.add(getResources().getDrawable(R.drawable.card10));
        images.add(getResources().getDrawable(R.drawable.card11));
        images.add(getResources().getDrawable(R.drawable.card12));
        images.add(getResources().getDrawable(R.drawable.card13));
        images.add(getResources().getDrawable(R.drawable.card14));
        images.add(getResources().getDrawable(R.drawable.card15));
        images.add(getResources().getDrawable(R.drawable.card16));
        images.add(getResources().getDrawable(R.drawable.card17));
        images.add(getResources().getDrawable(R.drawable.card18));
        images.add(getResources().getDrawable(R.drawable.card19));
        images.add(getResources().getDrawable(R.drawable.card20));
        images.add(getResources().getDrawable(R.drawable.card21));

    }

    private void loadCards() {
        try {
            int size = ROW_COUNT * COL_COUNT;

            Log.i("loadCards()", "size=" + size);

            ArrayList<Integer> list = new ArrayList<Integer>();

            for (int i = 0; i < size; i++) {
                list.add(new Integer(i));
            }


            Random r = new Random();

            for (int i = size - 1; i >= 0; i--) {
                int t = 0;

                if (i > 0) {
                    t = r.nextInt(i);
                }

                t = list.remove(t).intValue();
                cards[i % COL_COUNT][i / COL_COUNT] = t % (size / 2);

                Log.i("loadCards()", "card[" + (i % COL_COUNT) +
                        "][" + (i / COL_COUNT) + "]=" + cards[i % COL_COUNT][i / COL_COUNT]);
            }
        } catch (Exception e) {
            Log.e("loadCards()", e + "");
        }

    }

    private TableRow createRow(int y) {
        TableRow row = new TableRow(context);
        row.setHorizontalGravity(Gravity.CENTER);

        for (int x = 0; x < COL_COUNT; x++) {
            row.addView(createImageButton(x, y));
        }
        return row;
    }

    private View createImageButton(int x, int y) {
        Button button = new Button(context);
        button.setBackgroundDrawable(backImage);
        button.setId(100 * x + y);
        button.setOnClickListener(buttonListener);
        return button;
    }

    class ButtonListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            synchronized (lock) {
                if (firstCard != null && seconedCard != null) {
                    return;
                }
                int id = v.getId();
                int x = id / 100;
                int y = id % 100;
                turnCard((Button) v, x, y);
            }

        }

        private void turnCard(Button button, int x, int y) {
            button.setBackgroundDrawable(images.get(cards[x][y]));

            if (firstCard == null) {
                firstCard = new Card(button, x, y);
            } else {

                if (firstCard.x == x && firstCard.y == y) {
                    return; //the user pressed the same card
                }

                seconedCard = new Card(button, x, y);

                turns++;
                ((TextView) findViewById(R.id.tv1)).setText("Tries: " + turns);


                TimerTask tt = new TimerTask() {

                    @Override
                    public void run() {
                        try {
                            synchronized (lock) {
                                handler.sendEmptyMessage(0);
                            }
                        } catch (Exception e) {
                            Log.e("E1", e.getMessage());
                        }
                    }
                };

                Timer t = new Timer(false);
                t.schedule(tt, 1300);
            }


        }

    }

    class UpdateCardsHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            synchronized (lock) {
                checkCards();
            }
        }

        public void checkCards() {
            if (cards[seconedCard.x][seconedCard.y] == cards[firstCard.x][firstCard.y]) {
                //TODO: an diem
                score++;
                Log.e(TAG, "score: " + score);
                int size = COL_COUNT * ROW_COUNT;
                if (score == size / 2) {
                    score = 0;
                    int mainScore = 300 * size - turns * 100;
                    mainScore = mainScore < 0 ? 0 : mainScore;
                    Toast.makeText(Manager.this, " you win ", Toast.LENGTH_LONG).show();
                    tvScore.setText("Your score is " + mainScore);
                    tvScore.setVisibility(View.VISIBLE);
                    Log.i(TAG, "clientID: " + clientId);
                    if (mPlanetArcadeSDKHelper.isValidated()) {
                        mPlanetArcadeSDKHelper.submitScore(clientId, mainScore, 0, new OnScoreCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Submit Score Successfull");
                            }

                            @Override
                            public void onFailure(int i, String s) {

                            }
                        });
                    }
                }
                firstCard.button.setVisibility(View.INVISIBLE);
                seconedCard.button.setVisibility(View.INVISIBLE);
            } else {
                seconedCard.button.setBackgroundDrawable(backImage);
                firstCard.button.setBackgroundDrawable(backImage);
            }

            firstCard = null;
            seconedCard = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
//                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_PHONE_STATE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    startGame();
                } else {
                    // Permission Denied
                    Toast.makeText(this, "Some Permission is Denied", Toast.LENGTH_SHORT).show();
                    this.finish();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public boolean getListPermission(final Activity activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionsNeeded = new ArrayList<String>();

            final List<String> permissionsList = new ArrayList<String>();
            if (!addPermission(activity, permissionsList, Manifest.permission.READ_PHONE_STATE))
                permissionsNeeded.add("READ_PHONE_STATE");

            if (!addPermission(activity, permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
                permissionsNeeded.add("READ_EXTERNAL_STORAGE");

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    // Need Rationale
                    String message = getString(R.string.text_grant_access) + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++)
                        message = message + ", " + permissionsNeeded.get(i);
                    AlertDialog builder = new AlertDialog.Builder(activity)
                            .setTitle(message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Toast.makeText(Manager.this, "Some Permission is Denied", Toast.LENGTH_SHORT).show();
                                    Manager.this.finish();
                                }
                            }).create();
                    builder.show();

                    return false;
                }
                activity.requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean addPermission(Activity activity, List<String> permissionsList, String permission) {
        if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!activity.shouldShowRequestPermissionRationale(permission))
                return false;
        }
        return true;
    }

    private void openApp() {
        String packageName = "net.planetarcade.stag";
        boolean isInstalled = isIntalledApp(packageName, getApplicationContext());
        if (isInstalled) {
            Intent launcher = getPackageManager().getLaunchIntentForPackage(packageName);
            Bundle bundle = launcher.getExtras();
            startActivity(launcher);
        } else {
            Intent i = new Intent(android.content.Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://play.google.com/store/apps/details?id=net.planetarcade.stag"));
            startActivity(i);
        }
    }

    public boolean isIntalledApp(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean isInstalled;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            isInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            isInstalled = false;
        }
        return isInstalled;
    }
}