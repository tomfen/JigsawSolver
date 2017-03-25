package pt.jigsawsolver;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainMenuActivity extends Activity {
    ListView list;
    static{ System.loadLibrary("opencv_java3"); }

    Integer[] labelId = {
            R.string.fit_element,
            R.string.solve,
            R.string.settings,
    };
    Integer[] imageId = {
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        MainMenuList adapter = new MainMenuList(MainMenuActivity.this, labelId, imageId);

        list = (ListView)findViewById(R.id.MainMenuList);
        list.setAdapter(adapter);

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
