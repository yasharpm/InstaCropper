package com.yashoid.instacropper;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleUris {

    private static final String FAKE = "_MultipleUris";

    private List<Uri> mUris;

    public MultipleUris(Uri... uris) {
        mUris = new ArrayList<>(uris.length);

        Collections.addAll(mUris, uris);
    }

    public MultipleUris(List<Uri> uris) {
        mUris = new ArrayList<>(uris);
    }

    public MultipleUris(Uri uri) {
        mUris = new ArrayList<>(5);

        for (String name: uri.getQueryParameterNames()) {
            mUris.add(Uri.parse(uri.getQueryParameter(name)));
        }
    }

    public MultipleUris() {
        mUris = new ArrayList<>(5);
    }

    public MultipleUris add(Uri uri) {
        mUris.add(uri);
        return this;
    }

    public MultipleUris remove(Uri uri) {
        mUris.remove(uri);
        return this;
    }

    public List<Uri> getUris() {
        return mUris;
    }

    public int size() {
        return mUris.size();
    }

    public Uri toUri() {
        StringBuilder sb = new StringBuilder();

        sb.append(FAKE).append("://").append(FAKE).append("/?");

        for (int i = 0; i < mUris.size(); i++) {
            if (i > 0) {
                sb.append("&");
            }

            sb.append(i).append("=").append(mUris.get(i));
        }

        return Uri.parse(sb.toString());
    }

}
