package com.webcinema.mobile.model;

import org.json.JSONObject;

public class UserProfile {
    public final int id;
    public final String username;
    public final String email;
    public final String name;
    public final String birthday;
    public final String memberSince;
    public final int purchaseCount;
    public final double totalSpent;
    public final String lastPurchase;

    public UserProfile(JSONObject json) {
        id = json.optInt("id");
        username = json.optString("username");
        email = json.optString("email");
        name = json.optString("name");
        birthday = json.optString("birthday");
        memberSince = json.optString("member_since");
        purchaseCount = json.optInt("purchase_count");
        totalSpent = json.optDouble("total_spent");
        lastPurchase = json.optString("last_purchase");
    }
}
