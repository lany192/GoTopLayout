# GoTopLayout
init

[![](https://jitpack.io/v/lany192/GoTopLayout.svg)](https://jitpack.io/#lany192/GoTopLayout)
# Gradle 
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    
    
    dependencies {
        compile 'com.github.lany192:GoTopLayout:1.0.2'
    }
# Layout
    <com.lany.layout.GoTopLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:goto_top_icon="@drawable/go2top"
        app:goto_top_visible_item="10">
        <!--RecyclerView/ListView/GridView-->
        <android.support.v7.widget.RecyclerView
            android:id="@+id/recylcerview"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </com.lany.layout.GoTopLayout>