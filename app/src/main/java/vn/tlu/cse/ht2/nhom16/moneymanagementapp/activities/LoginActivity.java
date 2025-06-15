package vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // ActivityResultLauncher để xử lý kết quả đăng nhập Google
    private ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        // Đăng nhập Google thành công, xác thực với Firebase
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                        firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        // Đăng nhập Google thất bại, cập nhật giao diện người dùng phù hợp
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập Google bị hủy.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Liên kết với file bố cục XML

        mAuth = FirebaseAuth.getInstance();

        // Cấu hình Google Sign In để yêu cầu ID token
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // default_web_client_id được tạo tự động khi bạn kết nối Firebase với ứng dụng Android của mình
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Đặt lắng nghe sự kiện cho nút đăng nhập Google
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Kiểm tra xem người dùng đã đăng nhập chưa (non-null) và cập nhật giao diện người dùng tương ứng.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }
    }

    // Khởi tạo quy trình đăng nhập Google
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    // Xác thực Firebase với ID token của Google
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công, cập nhật giao diện người dùng với thông tin người dùng đã đăng nhập
                        Log.d(TAG, "signInWithCredential success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        updateUI(user);
                    } else {
                        // Nếu đăng nhập thất bại, hiển thị thông báo cho người dùng.
                        Log.w(TAG, "signInWithCredential failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Xác thực thất bại.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    // Chuyển hướng người dùng sau khi đăng nhập thành công hoặc thất bại
    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Người dùng đã đăng nhập, chuyển sang màn hình chính (MainActivity)
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng LoginActivity để người dùng không thể quay lại bằng nút back
        } else {
            // Người dùng chưa đăng nhập hoặc đăng xuất, có thể hiển thị lại nút đăng nhập hoặc các UI khác
        }
    }
}
