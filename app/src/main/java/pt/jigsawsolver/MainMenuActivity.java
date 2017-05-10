package pt.jigsawsolver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainMenuActivity extends Activity {
    ListView list;
    AdView adView;

    static{ System.loadLibrary("opencv_java3"); }

    Integer[] labelId = {
            R.string.fit_element,
            R.string.solve,
            R.string.settings,
    };
    Integer[] imageId = {
            R.drawable.ic_fit,
            R.drawable.ic_solve,
            R.drawable.ic_settings,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_menu);

        MainMenuList adapter = new MainMenuList(MainMenuActivity.this, labelId, imageId);

        adView = (AdView) findViewById(R.id.admob_adview);
        list = (ListView)findViewById(R.id.MainMenuList);
        list.setAdapter(adapter);

        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("91456216E0ECFE6F9134D159BFC820A8")
                .build();
        adView.loadAd(adRequest);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Intent i = null;

                switch(position) {
                    case 0:
                        i = new Intent(getApplicationContext(), HintActivity.class);
                        break;
                    case 1:
                        i = new Intent(getApplicationContext(), SolverActivity.class);
                        break;
                    case 2:
                        i = new Intent(getApplicationContext(), SettingsActivity.class);
                        break;
                    default:
                        break;
                }

                if (i != null)
                    startActivity(i);
            }
        });

    }
}
