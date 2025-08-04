package com.zcb.packinglistedit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;

public class MainActivity extends AppCompatActivity {
    private static final String Author = "qq379264347 (c) zcb 2025.08.03";
    private static final String FILE_DIR = "J2MECloud";
    private static final String FILE_NAME = "装箱单.txt";
    private static final int REQUEST_PERMISSION_CODE = 1001;
    private static final int REQUEST_MANAGE_STORAGE = 1002;
    /**
     * 顶部状态文本视图
     */
    private TextView statusTextView;
    /**
     * 数字搜索框
     */
    private EditText searchEditText;
    /**
     * 文本搜索框
     */
    private EditText textSearchEditText;
    /**
     * 查询按钮
     */
    private Button searchButton;
    /**
     * 隐藏已装箱复选框
     */
    private CheckBox hideBoxedCheckBox;
    /**
     * 放入上一个箱号按钮
     */
    private Button prevBoxButton;
    /**
     * 放入新箱号按钮
     */
    private Button newBoxButton;
    /**
     * 指定箱号按钮
     */
    private Button customBoxButton;
    /**
     * 高性能动态列表组件
     */
    private RecyclerView partsRecyclerView;

    /**
     * 所有零件信息列表
     */
    private List<Part> allParts = new ArrayList<>();
    /**
     * 当前显示的零件信息列表
     */
    private List<Part> displayedParts = new ArrayList<>();
    /**
     * 零件信息数据展示适配器
     */
    private PartAdapter adapter;
    /**
     * 下一个新箱号，若都没编号则为1，否则为：最大箱号+1
     */
    private int nextNewBoxNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews(); //初始化视图
        setupListeners(); //设置监听器

        // 检查并请求权限
        checkAndRequestPermissions();
    }

    /**
     * 检查并请求应用所需的运行时权限
     *
     * 功能流程：
     * 1. 根据 Android 版本执行不同的权限检查逻辑
     * 2. Android 6.0+ (API 23+) 检查单个/多个权限
     * 3. Android 11+ (API 30+) 特殊处理 MANAGE_EXTERNAL_STORAGE
     * 4. 无权限时发起请求，有权限则直接执行业务逻辑
     *
     * 1. Android 版本分层处理
     * Android 版本	处理方式
     * API 30+ (Android 11+)	检查 MANAGE_EXTERNAL_STORAGE 权限
     * API 23-29 (Android 6.0-10)	检查 READ/WRITE_EXTERNAL_STORAGE
     * API <23 (Android 5.1-)	默认拥有权限（无需处理）
     */
    private void checkAndRequestPermissions() {
        // ===================== 1. Android 11+ 特殊处理 =====================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 1.1 检查是否已获得「所有文件访问权限」（MANAGE_EXTERNAL_STORAGE）
            if (!Environment.isExternalStorageManager()) {
                // 1.2 跳转系统设置页面引导用户开启权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_STORAGE);
            } else {
                loadData();
            }
        } else { //Build.VERSION.SDK_INT >= Build.VERSION_CODES.M需判断，API<23(Android 5.1-)默认拥有权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        REQUEST_PERMISSION_CODE);
            } else {
                loadData();
            }
        }
    }

    /**
     * 当以下两个条件同时满足时，系统会自动调用此方法：
     * 1、通过 ActivityCompat.requestPermissions() 或 requestPermissions() 请求了危险权限（Dangerous Permission）
     * 2、用户做出了权限授予或拒绝的选择（包括永久拒绝）
     * @param requestCode 自定义请求标识符（需与 requestPermissions() 的请求码一致）
     * @param permissions 被请求的权限数组（如 Manifest.permission.CAMERA）从不为空
     * @param grantResults 对应权限的授予结果（PackageManager.PERMISSION_GRANTED 或 PERMISSION_DENIED）从不为空
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData();
            } else {
                Toast.makeText(this, "需要存储权限才能运行应用", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 当通过 startActivityForResult() 启动另一个 Activity，且该 Activity 调用 setResult() 后返回时，系统会自动回调此方法。
     * @param requestCode 与 startActivityForResult() 的请求码一致，用于区分不同请求
     * @param resultCode 结果状态（通常为 RESULT_OK 或 RESULT_CANCELED）
     * @param data 携带返回数据的 Intent（可能为 null）
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) { //判断调用应用程序是否在主共享/外部存储介质上具有所有文件访问权限
                    loadData();
                } else {
                    Toast.makeText(this, "需要所有文件访问权限才能运行应用", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 初始化视图
     */
    private void initializeViews() {
        statusTextView = findViewById(R.id.statusTextView);
        searchEditText = findViewById(R.id.searchEditText);
        textSearchEditText = findViewById(R.id.textSearchEditText);
        searchButton = findViewById(R.id.searchButton);
        hideBoxedCheckBox = findViewById(R.id.hideBoxedCheckBox);
        prevBoxButton = findViewById(R.id.prevBoxButton);
        newBoxButton = findViewById(R.id.newBoxButton);
        customBoxButton = findViewById(R.id.customBoxButton);
        partsRecyclerView = findViewById(R.id.partsRecyclerView);

        partsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PartAdapter(displayedParts);
        partsRecyclerView.setAdapter(adapter);
    }

    /**
     * 设置监听器
     */
    private void setupListeners() {
        searchButton.setOnClickListener(v -> performSearch());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        textSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        hideBoxedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> performSearch());

        prevBoxButton.setOnClickListener(v -> assignToBox(nextNewBoxNumber - 1));
        newBoxButton.setOnClickListener(v -> assignToBox(nextNewBoxNumber));
        customBoxButton.setOnClickListener(v -> showCustomBoxDialog());
    }

    /**
     * 获取存储文件的绝对路径对象：
     * 1. 确定文件存储目录（根目录/J2MECloud）
     * 2. 确保目录存在（不存在则创建）
     * 3. 返回指定文件（装箱单.txt）的 File 对象
     *
     * @return File 对象，指向 /storage/emulated/0/J2MECloud/装箱单.txt
     *
     * 注意：需要确保调用此方法前已获得存储权限（MANAGE_EXTERNAL_STORAGE 或 WRITE_EXTERNAL_STORAGE）
     */
    private File getStorageFile() {
        // 1. 获取外部存储根目录（通常为 /storage/emulated/0）
        File rootDir = Environment.getExternalStorageDirectory();
        // 2. 构建应用专属子目录 File 对象（/storage/emulated/0/J2MECloud）
        File appDir = new File(rootDir, FILE_DIR);
        // 3. 检查目录是否存在，不存在则创建（包括父目录）
        if (!appDir.exists()) {
            appDir.mkdirs(); // 创建多级目录
        }
        // 4. 返回目标文件对象（/storage/emulated/0/J2MECloud/装箱单.txt）
        return new File(appDir, FILE_NAME);
    }

    /**
     * 加载存储的零件数据文件（/J2MECloud/装箱单.txt），并解析成 Part 对象列表
     * 1. 检查文件是否存在 → 不存在则创建示例数据
     * 2. 读取文件内容，按行解析（格式：保修单编号\t装箱单箱号\t配件代码\t配件名称\t配件数量）
     * 3. 更新当前最大箱号（用于自动计算新箱号）
     * 4. 触发搜索刷新界面显示
     */
    private void loadData() {
        // 1. 获取目标文件对象（/J2MECloud/装箱单.txt）
        File file = getStorageFile();

        // 2. 如果文件不存在，创建示例数据并终止后续逻辑
        if (!file.exists()) {
            createSampleData(); // 调用创建示例数据方法
            return; // 终止执行
        }

        // 3. 初始化临时变量
        allParts.clear();       // 清空现有数据
        int maxBoxNumber = 0;   // 记录最大箱号（用于计算新箱号）

        // 4. 读取文件内容（使用try-with-resources自动关闭流）
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(file),
                        StandardCharsets.UTF_8))) { //V1.1 指定UTF-8
            String line;
            while ((line = reader.readLine()) != null) { // 逐行读取
                line = line.trim(); // 去除首尾空格
                if (line.isEmpty()) continue; // 跳过空行

                // 5. 按Tab分割每行数据
                String[] parts = line.split("\t");
                //数据格式错误检查，5列内容且不是标题行
                if (parts.length == 5 && !"保修单号".equals(parts[0])) { //0保修单号 1箱号 2旧件编号 3旧件名称 4发运量
                    String boxNumber = parts[1].trim();
                    // 6. 解析数据并创建Part对象
                    allParts.add(new Part(parts[0].trim(), boxNumber, parts[2].trim(), parts[3].trim(), parts[4].trim()));

                    // 7. 更新最大箱号（仅处理数字箱号）
                    if (!boxNumber.isEmpty()) {
                        try {
                            int num = Integer.parseInt(boxNumber);
                            if (num > maxBoxNumber) {
                                maxBoxNumber = num;
                            }
                        } catch (NumberFormatException e) {
                            // 忽略非数字箱号
                        }
                    }
                }
            }

            // 8. 计算下一个箱号（当前最大箱号+1）
            if (maxBoxNumber > 0) {
                nextNewBoxNumber = maxBoxNumber + 1;
            } else {
                nextNewBoxNumber = 1;
            }

            // 10. 触发搜索刷新界面
            performSearch();
            updateStatus(); // 更新顶部状态栏
        } catch (IOException e) {
            Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 创建示例文件数据
     */
    private void createSampleData() {
        File file = getStorageFile();
        try (OutputStream os = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            String sampleData =
                    "保修单号\t箱号\t旧件编号\t旧件名称\t发运量\n" +
                    "2025070002\t\tF4J15-1121010\t油轨喷油器总成\t1\n" +
                    "2025070003\t\tF4J16-1130011\t碳罐电磁阀总成\t1\n" +
                    "2025070004\t\t807000877AA\t车载充电器总成\t1\n";
            writer.write(sampleData);

            loadData(); // 重新加载新创建的数据
        } catch (IOException e) {
            Toast.makeText(this, "创建示例文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 持久化保存数据到磁盘文件
     */
    private void saveData() {
        File file = getStorageFile();
        try (OutputStream os = new FileOutputStream(file);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            //写标题
            String title = "保修单号\t箱号\t旧件编号\t旧件名称\t发运量\n";
            writer.write(title);
            for (Part part : allParts) {
                String line = part.getBxid() + "\t" + part.getBoxNumber() + "\t" + part.getCode() + "\t" + part.getName() + "\t" + part.getNum() + "\n";
                writer.write(line);
            }
        } catch (IOException e) {
            Toast.makeText(this, "保存文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 执行搜索和过滤操作，更新列表显示：
     * 1. 根据输入框内容筛选零件（按序号或名称模糊匹配）
     * 2. 根据复选框状态过滤已装箱的零件
     * 3. 刷新 RecyclerView 显示结果
     *  * 高级搜索逻辑：
     *  * 1. 如果数字框有内容，按数字搜索
     *  * 2. 如果文本框有内容，按文本搜索
     *  * 3. 如果都有内容，则同时满足两个条件
     */
    private void performSearch() {
        // 1. 获取搜索关键词（转换为小写方便忽略大小写匹配）
//        String query = searchEditText.getText().toString().trim().toLowerCase();
        String numberQuery = searchEditText.getText().toString().trim().toLowerCase();
        String textQuery = textSearchEditText.getText().toString().trim().toLowerCase();
        // 2. 获取复选框状态：是否隐藏已装箱的零件
        boolean hideBoxed = hideBoxedCheckBox.isChecked();

        // 3. 清空当前显示的数据列表
        displayedParts.clear();

        // 4. 遍历所有零件数据，逐个匹配筛选条件
        for (Part part : allParts) {
            // 条件1：检查是否匹配搜索关键词（匹配序号或名称）
            boolean matchesQuery = (numberQuery.isEmpty() && textQuery.isEmpty()) || // 无关键词时显示所有
                    //配件号与名称同时匹配
                    (pipeixuhao(numberQuery, part.getBxid().toLowerCase()) && // 扩容4位后去匹配序号 part.getBxid().toLowerCase().contains(query)结果多
                    pipeimingcheng(textQuery, part.getName().toLowerCase())); // 不是纯数字时候去匹配名称 part.getName().toLowerCase().contains(query)

            // 条件2：检查是否已装箱（根据复选框状态决定是否过滤）
            boolean isBoxed = !part.getBoxNumber().isEmpty(); // 箱号非空=已装箱
            boolean matchesFilter = !hideBoxed || !isBoxed;  // 如果勾选"隐藏已装箱"，则只显示未装箱的

            // 5. 如果同时满足搜索和过滤条件，则添加到显示列表
            if (matchesQuery && matchesFilter) {
                displayedParts.add(part);
            }
        }

        // 6. 通知适配器数据已变更，刷新 RecyclerView
        adapter.updateData(displayedParts);
    }

    /**
     * 匹配名称逻辑，不是纯数字时候才匹配
     * @param query 输入的内容-假如是文本
     * @param name 清单中配件名称，如：碳罐电磁阀总成
     * @return 是否匹配上，匹配上返回true，即为检索成功
     */
    private boolean pipeimingcheng(String query, String name) {
        if (query.isEmpty()) { //为空的话就返回true，进行显示
            return true;
        }
        // 检查 query 是否由纯数字组成（不用正则，逐字符判断）
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c >= '0' && c <= '9') {
                return false; // 是数字字符，直接返回 false
            }
        }
        return name.contains(query);
    }

    /**
     * 匹配序号逻辑
     * @param query 输入的内容-假如是数字
     * @param bxid 清单中保修单号，如：2025070002
     * @return 是否匹配上，保修单号后面数字是输入的数字编号，匹配上返回true，即为检索成功
     */
    private boolean pipeixuhao(String query, String bxid) {
        if (query.isEmpty()) { //为空的话就返回true，进行显示
            return true;
        }
        // 检查query是否由纯数字组成
//        if (!query.matches("[0-9]+")) {
//            return false;
//        }

        // 1. 检查 query 是否由纯数字组成（不用正则，逐字符判断）
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c < '0' || c > '9') {
                return false; // 不是数字字符，直接返回 false
            }
        }

        // 检查query长度是否超过4位
        if (query.length() > 4) {
            return false;
        }

        // 3. 手动补零到4位（不用 String.format） String.format("%04d", Integer.parseInt(query))
        StringBuilder paddedQuery = new StringBuilder(query);
        while (paddedQuery.length() < 4) {
            paddedQuery.insert(0, '0'); // 前面补0
        }

        // 4. 检查 bxid 是否有4位，并比较后4位
        if (bxid.length() >= 4) {
            String bxidSuffix = bxid.substring(bxid.length() - 4);
            return bxidSuffix.equals(paddedQuery.toString());
        }

        return false;
    }

    /**
     * 将当前列表中显示的零件分配到指定箱号，并更新数据文件
     *
     * @param boxNumber 目标箱号（必须为正整数）
     *
     * 功能流程：
     * 1. 检查是否有可分配的零件
     * 2. 更新所有显示零件的箱号
     * 3. 计算下一个建议箱号（当前箱号+1）
     * 4. 保存数据到文件
     * 5. 刷新界面显示
     * 6. 显示操作结果提示
     * 新增不同箱号覆盖弹窗二次确认提示
     */
    private void assignToBox(int boxNumber) {
        // === 1. 安全检查 ===
        // 检查当前显示列表是否为空
        if (displayedParts.isEmpty()) {
            Toast.makeText(this, "没有可分配箱号的条目", Toast.LENGTH_SHORT).show();
            return;
        }

        if (boxNumber < 0) {
            Toast.makeText(this, "箱号不能小于0", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 检查是否有已分配不同箱号的项
        boolean willOverwrite = false;
        for (Part part : displayedParts) {
            if (!part.getBoxNumber().isEmpty() &&
                    !part.getBoxNumber().equals(String.valueOf(boxNumber))) {
                willOverwrite = true;
                break;
            }
        }

        // 3. 如果有覆盖风险，弹窗确认
        if (willOverwrite) {
            new AlertDialog.Builder(this)
                    .setTitle("注意")
                    .setMessage("将覆盖已有箱号，确认继续吗？")
                    .setPositiveButton("确认", (dialog, which) -> {
                        executeBoxAssignment(boxNumber); // 用户确认后执行
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            executeBoxAssignment(boxNumber); // 无风险直接执行
        }
    }

    /**
     * 实际执行箱号分配（原逻辑迁移到这里）
     */
    private void executeBoxAssignment(int boxNumber) {
        // === 2. 更新零件数据 ===
        // 遍历当前显示的所有零件（已通过搜索/过滤后的结果）
        for (Part part : displayedParts) {
            // 设置箱号（转换为字符串存储）
            part.setBoxNumber(String.valueOf(boxNumber));
        }

        // === 3. 更新箱号计数器 ===
        // 下一个建议箱号 = 当前分配箱号 + 1
        nextNewBoxNumber = boxNumber + 1;
        // === 4. 持久化存储 ===
        saveData(); // 将修改后的 allParts 保存到文件

        // === 6. 用户反馈 === 先反馈获取displayedParts.size()数量
        Toast.makeText(this,
                "已分配 " + displayedParts.size() + " 个零件到箱号: " + boxNumber,
                Toast.LENGTH_SHORT).show();

        // === 5. 界面刷新 ===
        performSearch(); // 重新执行搜索（可能受"隐藏已装箱"选项影响）
        updateStatus();  // 更新顶部状态栏显示
    }

    /**
     * 显示自定义箱号输入对话框：
     * 1. 弹出带输入框的对话框
     * 2. 用户输入数字箱号（正整数）
     * 3. 点击确认后验证并分配箱号
     * 4. 点击取消则关闭对话框
     */
    private void showCustomBoxDialog() {
        // 1. 创建对话框布局中的输入框
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER); // 仅允许数字输入
        input.setHint("请输入箱号（正整数）");

        // 2. 设置输入框的默认值为当前建议箱号
//        input.setText(String.valueOf(nextNewBoxNumber));

        // 3. 构建AlertDialog
        new AlertDialog.Builder(this)
                .setTitle("分配至指定箱号")
                .setMessage("请输入要分配的箱号：")
                .setView(input) // 添加输入框
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 4. 获取用户输入的箱号
                        String boxNumberStr = input.getText().toString().trim();

                        // 5. 验证输入是否有效
                        if (boxNumberStr.isEmpty()) {
                            Toast.makeText(MainActivity.this, "箱号不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            int boxNumber = Integer.parseInt(boxNumberStr);
                            if (boxNumber < 0) {
                                Toast.makeText(MainActivity.this, "箱号必须为非负整数", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // 6. 验证通过，执行分配操作
                            assignToBox(boxNumber);
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "请输入有效数字", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // 关闭对话框
                    }
                })
                .create()
                .show();
    }

    /**
     * 更新顶部状态栏信息
     */
    private void updateStatus() {
        int lastBoxNumber = nextNewBoxNumber - 1;

        String status = "上一次装箱号: " + lastBoxNumber +
                "\n下一个新箱号: " + nextNewBoxNumber;
        statusTextView.setText(status);
    }

//    /**
//     * 统计指定箱号的零件数量  使用方式：int partCount = countPartsInBox(lastBoxNumber);
//     * @param boxNumber
//     * @return
//     */
//    private int countPartsInBox(int boxNumber) {
//        int count = 0;
//        for (Part part : allParts) {
//            try {
//                if (Integer.parseInt(part.getBoxNumber()) == boxNumber) {
//                    count++;
//                }
//            } catch (NumberFormatException e) {
//                // 忽略非数字箱号
//            }
//        }
//        return count;
//    }
}