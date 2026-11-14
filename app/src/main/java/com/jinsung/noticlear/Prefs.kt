package com.jinsung.noticlear

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("noticlear", Context.MODE_PRIVATE)
    fun alpha(c: Context) = sp(c).getInt("alpha", 85)          // 20~100 %
    fun setAlpha(c: Context, v: Int) = sp(c).edit().putInt("alpha", v).apply()
    fun size(c: Context) = sp(c).getInt("size", 56)            // 40~80 dp
    fun setSize(c: Context, v: Int) = sp(c).edit().putInt("size", v).apply()
    fun pos(c: Context) = Pair(sp(c).getInt("x", -1), sp(c).getInt("y", 400))
    fun setPos(c: Context, x: Int, y: Int) = sp(c).edit().putInt("x", x).putInt("y", y).apply()
    fun register(c: Context, l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).registerOnSharedPreferenceChangeListener(l)
    fun unregister(c: Context, l: android.content.SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).unregisterOnSharedPreferenceChangeListener(l)
}
