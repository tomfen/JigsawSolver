package pt.jigsawsolver;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MainMenuList extends ArrayAdapter<Integer>{

    private final Activity context;
    private final Integer[] labelId;
    private final Integer[] imageId;

    public MainMenuList(Activity context, Integer[] labelId, Integer[] imageId) {
        super(context, R.layout.main_menu_item, labelId);
        this.context = context;
        this.labelId = labelId;
        this.imageId = imageId;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.main_menu_item, null, true);
        TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);

        ImageView imageView = (ImageView) rowView.findViewById(R.id.img);

        txtTitle.setText(labelId[position]);

        imageView.setImageResource(imageId[position]);
        return rowView;
    }
}