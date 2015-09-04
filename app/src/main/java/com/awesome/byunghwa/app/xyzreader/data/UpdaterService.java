package com.awesome.byunghwa.app.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.awesome.byunghwa.app.xyzreader.remote.RemoteEndpointUtil;
import com.awesome.byunghwa.app.xyzreader.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.awesome.byunghwa.app.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.awesome.byunghwa.app.xyzreader.intent.extra.REFRESHING";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }

        // set to true to initiate refreshing indicator
        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array" );
            }

            LogUtil.log_i(TAG, "result json array: " + array.toString()); // result json array: [{"id":"1","photo":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/images\/p1.jpg","thumb":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/thumbs\/p1.jpg","author":"Isaac Newton","title":"1. Bacon spam ipsum dolor sit amet","published_date":"2013-06-20T00:00:00.000Z","body":"Bacon ipsum dolor sit amet <a href='foo.html'>frankfurter tenderloin<\/a> beef ribs pig turducken, tail jowl cow bresaola pork shoulder pastrami short ribs drumstick strip steak.<br><br>Beef ribs kielbasa sirloin pork loin chicken pork chop rump andouille tail. Beef ribs corned beef sausage, doner shoulder capicola pork pastrami jowl chuck shankle. T-bone ribeye chicken turducken drumstick rump prosciutto tri-tip pork belly sausage shankle venison shoulder pastrami ball tip.<br><br>Frankfurter ball tip pork belly shoulder short loin. Boudin andouille ham hock tri-tip tail, capicola t-bone fatback kielbasa venison cow drumstick ribeye biltong. Shoulder ribeye hamburger, pork belly strip steak chuck spare ribs ham hock salami. Turkey filet mignon t-bone, ribeye tail boudin jowl short loin andouille spare ribs. Cow tri-tip ball tip chuck, leberkas venison meatball pastrami salami short loin bresaola. Turducken sirloin turkey ribeye bresaola jowl bacon meatloaf sausage.<br><br>Brisket doner tail capicola. Ham swine biltong jowl ribeye jerky tenderloin pork belly hamburger venison brisket. Capicola ground round pancetta jowl, turducken pork belly doner venison spare ribs boudin frankfurter. Cow swine ball tip jowl, hamburger salami prosciutto biltong ribeye venison tail short loin chuck turkey. Leberkas fatback tongue, shoulder prosciutto strip steak ground round short ribs kielbasa short loin flank. Meatball drumstick turkey pork loin. Cow spare ribs chuck, beef ribs tongue ham salami swine drumstick capicola jowl sirloin pork bresaola.","aspect_ratio":1.503671072},
                                                                                                // {"id":"2","photo":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/images\/p2.jpg","thumb":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/thumbs\/p2.jpg","author":"Margaret Thatcher","title":"6. Tri-tip spare ribs pancetta t-bone short ribs meatball","published_date":"2013-01-19T00:00:00.000Z","body":"Bacon ipsum dolor sit amet <a href='foo.html'>frankfurter tenderloin<\/a> beef ribs pig turducken, tail jowl cow bresaola pork shoulder pastrami short ribs drumstick strip steak.<br><br>Beef ribs kielbasa sirloin pork loin chicken pork chop rump andouille tail. Beef ribs corned beef sausage, doner shoulder capicola pork pastrami jowl chuck shankle. T-bone ribeye chicken turducken drumstick rump prosciutto tri-tip pork belly sausage shankle venison shoulder pastrami ball tip.<br><br>Frankfurter ball tip pork belly shoulder short loin. Boudin andouille ham hock tri-tip tail, capicola t-bone fatback kielbasa venison cow drumstick ribeye biltong. Shoulder ribeye hamburger, pork belly strip steak chuck spare ribs ham hock salami. Turkey filet mignon t-bone, ribeye tail boudin jowl short loin andouille spare ribs. Cow tri-tip ball tip chuck, leberkas venison meatball pastrami salami short loin bresaola. Turducken sirloin turkey ribeye bresaola jowl bacon meatloaf sausage.<br><br>Brisket doner tail capicola. Ham swine biltong jowl ribeye jerky tenderloin pork belly hamburger venison brisket. Capicola ground round pancetta jowl, turducken pork belly doner venison spare ribs boudin frankfurter. Cow swine ball tip jowl, hamburger salami prosciutto biltong ribeye venison tail short loin chuck turkey. Leberkas fatback tongue, shoulder prosciutto strip steak ground round short ribs kielbasa short loin flank. Meatball drumstick turkey pork loin. Cow spare ribs chuck, beef ribs tongue ham salami swine drumstick capicola jowl sirloin pork bresaola.","aspect_ratio":0.666666667},{"id":"3","photo":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/images\/p3.jpg","thumb":"https:\/\/dl.dropboxusercontent.com\/u\/231329\/xyzreader_data\/thumbs\/p3.jpg","author":"Vincent Van Gogh","

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id" ));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author" ));
                values.put(ItemsContract.Items.TITLE, object.getString("title" ));
                values.put(ItemsContract.Items.BODY, object.getString("body" ));
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb" ));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo" ));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio" ));
                time.parse3339(object.getString("published_date"));
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        // set to false to cancel refreshing
        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
