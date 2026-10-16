package com.dafanmod.dafan4k;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private Uri selectedUri;
    private Bitmap sourceBitmap;
    private ImageView preview;
    private TextView status;
    private Button upscaleButton;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFFF1F5F9);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(0xFF07111F);
        scroll.addView(root);

        root.addView(text("DAFAN 4K", 30));
        root.addView(text("استوديو تحسين الصور", 18));
        root.addView(text("اختر صورة ثم أنشئ نسخة بأبعاد تصل إلى 3840×2160.", 14));

        Button pick = button("📁 اختيار صورة");
        root.addView(pick);
        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE);
        });

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFF132238);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, dp(300));
        imageParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(preview, imageParams);

        upscaleButton = button("✨ تكبير الصورة إلى 4K");
        root.addView(upscaleButton);
        upscaleButton.setEnabled(false);
        upscaleButton.setOnClickListener(v -> upscaleAndSave());

        status = text("الحالة: بانتظار اختيار صورة", 14);
        root.addView(status);
        root.addView(text("ملاحظة: هذه النسخة تستخدم تكبير Bitmap محليًا، وليست نموذج ذكاء اصطناعي لاستعادة التفاصيل.", 12));
        setContentView(scroll);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData();
            try (InputStream in = getContentResolver().openInputStream(selectedUri)) {
                sourceBitmap = BitmapFactory.decodeStream(in);
                if (sourceBitmap == null) throw new IOException("تعذر قراءة الصورة");
                preview.setImageBitmap(sourceBitmap);
                status.setText("الصورة: " + sourceBitmap.getWidth() + " × " + sourceBitmap.getHeight());
                upscaleButton.setEnabled(true);
            } catch (Exception e) {
                status.setText("تعذر فتح الصورة: " + e.getMessage());
                Toast.makeText(this, "تعذر فتح الصورة", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void upscaleAndSave() {
        if (sourceBitmap == null) return;
        try {
            int sw = sourceBitmap.getWidth(), sh = sourceBitmap.getHeight();
            // Fit within 4K canvas without distorting aspect ratio or upscaling already-larger images.
            float scale = Math.min(3840f / sw, 2160f / sh);
            if (scale <= 1f) {
                status.setText("الصورة أصلًا كبيرة؛ لن يتم تصغيرها. استخدم صورة أصغر من 4K.");
                Toast.makeText(this, "الصورة لا تحتاج إلى تكبير 4K", Toast.LENGTH_LONG).show();
                return;
            }
            int w = Math.max(1, Math.round(sw * scale));
            int h = Math.max(1, Math.round(sh * scale));
            Bitmap result = Bitmap.createScaledBitmap(sourceBitmap, w, h, true);
            String name = "Dafan4K_" + System.currentTimeMillis() + ".jpg";
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dafan4K");
            Uri outUri = getContentResolver().insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (outUri == null) throw new IOException("تعذر إنشاء ملف الإخراج");
            try (OutputStream out = getContentResolver().openOutputStream(outUri)) {
                if (out == null || !result.compress(Bitmap.CompressFormat.JPEG, 95, out))
                    throw new IOException("فشل حفظ الصورة");
            }
            preview.setImageBitmap(result);
            status.setText("تم الحفظ في Pictures/Dafan4K\nالأبعاد: " + w + " × " + h);
            Toast.makeText(this, "تم حفظ الصورة المحسنة", Toast.LENGTH_LONG).show();
        } catch (OutOfMemoryError e) {
            status.setText("الذاكرة غير كافية لمعالجة هذه الصورة. جرّب صورة أصغر.");
        } catch (Exception e) {
            status.setText("فشلت المعالجة: " + e.getMessage());
        }
    }
}
