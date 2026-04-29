import java.util.*;
import java.util.function.*;

public class UserManagementSystem {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    用户管理系统 - 设计模式演示");
        System.out.println("========================================");
        System.out.println();
        
        UserManager manager = UserManager.getInstance();
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n请选择操作:");
            System.out.println("1. 创建用户");
            System.out.println("2. 查询用户");
            System.out.println("3. 修改用户");
            System.out.println("4. 删除用户");
            System.out.println("5. 列出所有用户");
            System.out.println("6. 按条件筛选用户 (策略模式)");
            System.out.println("7. 撤销操作 (命令模式)");
            System.out.println("0. 退出");
            System.out.print("请输入选项: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    createUser(manager, scanner);
                    break;
                case "2":
                    queryUser(manager, scanner);
                    break;
                case "3":
                    updateUser(manager, scanner);
                    break;
                case "4":
                    deleteUser(manager, scanner);
                    break;
                case "5":
                    listAllUsers(manager);
                    break;
                case "6":
                    filterUsers(manager, scanner);
                    break;
                case "7":
                    undoOperation(manager);
                    break;
                case "0":
                    System.out.println("感谢使用，再见！");
                    return;
                default:
                    System.out.println("无效选项，请重新输入。");
            }
        }
    }
    
    private static void createUser(UserManager manager, Scanner scanner) {
        System.out.println("\n--- 创建用户 ---");
        System.out.print("请输入用户ID: ");
        String id = scanner.nextLine();
        System.out.print("请输入用户名: ");
        String name = scanner.nextLine();
        System.out.print("请输入邮箱: ");
        String email = scanner.nextLine();
        System.out.print("请输入年龄: ");
        int age = Integer.parseInt(scanner.nextLine());
        
        UserFactory factory = new UserFactory();
        User user = factory.createUser(id, name, email, age);
        
        Command command = new CreateUserCommand(manager, user);
        manager.executeCommand(command);
        
        System.out.println("用户创建成功: " + user);
    }
    
    private static void queryUser(UserManager manager, Scanner scanner) {
        System.out.println("\n--- 查询用户 ---");
        System.out.print("请输入用户ID: ");
        String id = scanner.nextLine();
        
        User user = manager.getUser(id);
        if (user != null) {
            System.out.println("找到用户: " + user);
        } else {
            System.out.println("未找到ID为 " + id + " 的用户");
        }
    }
    
    private static void updateUser(UserManager manager, Scanner scanner) {
        System.out.println("\n--- 修改用户 ---");
        System.out.print("请输入用户ID: ");
        String id = scanner.nextLine();
        
        User user = manager.getUser(id);
        if (user == null) {
            System.out.println("未找到ID为 " + id + " 的用户");
            return;
        }
        
        System.out.println("当前用户信息: " + user);
        System.out.print("请输入新用户名 (直接回车保持不变): ");
        String name = scanner.nextLine();
        System.out.print("请输入新邮箱 (直接回车保持不变): ");
        String email = scanner.nextLine();
        System.out.print("请输入新年龄 (直接回车保持不变): ");
        String ageStr = scanner.nextLine();
        
        String newName = name.isEmpty() ? user.getName() : name;
        String newEmail = email.isEmpty() ? user.getEmail() : email;
        int newAge = ageStr.isEmpty() ? user.getAge() : Integer.parseInt(ageStr);
        
        Command command = new UpdateUserCommand(manager, id, newName, newEmail, newAge);
        manager.executeCommand(command);
        
        System.out.println("用户更新成功: " + manager.getUser(id));
    }
    
    private static void deleteUser(UserManager manager, Scanner scanner) {
        System.out.println("\n--- 删除用户 ---");
        System.out.print("请输入用户ID: ");
        String id = scanner.nextLine();
        
        User user = manager.getUser(id);
        if (user == null) {
            System.out.println("未找到ID为 " + id + " 的用户");
            return;
        }
        
        Command command = new DeleteUserCommand(manager, id);
        manager.executeCommand(command);
        
        System.out.println("用户删除成功: " + user.getName());
    }
    
    private static void listAllUsers(UserManager manager) {
        System.out.println("\n--- 所有用户 ---");
        List<User> users = manager.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("暂无用户");
        } else {
            for (User user : users) {
                System.out.println(user);
            }
            System.out.println("共 " + users.size() + " 个用户");
        }
    }
    
    private static void filterUsers(UserManager manager, Scanner scanner) {
        System.out.println("\n--- 筛选用户 (策略模式) ---");
        System.out.println("1. 按年龄筛选");
        System.out.println("2. 按姓名筛选");
        System.out.println("3. 按邮箱筛选");
        System.out.print("请选择筛选方式: ");
        String choice = scanner.nextLine();
        
        UserFilterStrategy strategy = null;
        switch (choice) {
            case "1":
                System.out.print("请输入最小年龄: ");
                int minAge = Integer.parseInt(scanner.nextLine());
                System.out.print("请输入最大年龄: ");
                int maxAge = Integer.parseInt(scanner.nextLine());
                strategy = new AgeFilterStrategy(minAge, maxAge);
                break;
            case "2":
                System.out.print("请输入姓名关键字: ");
                String keyword = scanner.nextLine();
                strategy = new NameFilterStrategy(keyword);
                break;
            case "3":
                System.out.print("请输入邮箱域名: ");
                String domain = scanner.nextLine();
                strategy = new EmailFilterStrategy(domain);
                break;
            default:
                System.out.println("无效选项");
                return;
        }
        
        List<User> filtered = manager.filterUsers(strategy);
        System.out.println("筛选结果:");
        for (User user : filtered) {
            System.out.println(user);
        }
        System.out.println("共 " + filtered.size() + " 个用户符合条件");
    }
    
    private static void undoOperation(UserManager manager) {
        System.out.println("\n--- 撤销操作 (命令模式) ---");
        if (manager.undo()) {
            System.out.println("撤销成功");
        } else {
            System.out.println("没有可撤销的操作");
        }
    }
}

class User {
    private String id;
    private String name;
    private String email;
    private int age;
    
    public User(String id, String name, String email, int age) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
    
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setAge(int age) { this.age = age; }
    
    @Override
    public String toString() {
        return "User[id=" + id + ", name=" + name + ", email=" + email + ", age=" + age + "]";
    }
}

class UserFactory {
    private static int idCounter = 1;
    
    public User createUser(String id, String name, String email, int age) {
        if (id == null || id.isEmpty()) {
            id = "U" + String.format("%04d", idCounter++);
        }
        return new User(id, name, email, age);
    }
    
    public User createDefaultUser() {
        return new User("U0000", "默认用户", "default@example.com", 0);
    }
}

interface Command {
    void execute();
    void undo();
}

class CreateUserCommand implements Command {
    private UserManager manager;
    private User user;
    
    public CreateUserCommand(UserManager manager, User user) {
        this.manager = manager;
        this.user = user;
    }
    
    @Override
    public void execute() {
        manager.addUserDirectly(user);
        manager.notifyObservers("CREATE", user);
    }
    
    @Override
    public void undo() {
        manager.removeUserDirectly(user.getId());
        manager.notifyObservers("UNDO_CREATE", user);
    }
}

class UpdateUserCommand implements Command {
    private UserManager manager;
    private String id;
    private String newName;
    private String newEmail;
    private int newAge;
    private User oldUser;
    
    public UpdateUserCommand(UserManager manager, String id, String newName, String newEmail, int newAge) {
        this.manager = manager;
        this.id = id;
        this.newName = newName;
        this.newEmail = newEmail;
        this.newAge = newAge;
    }
    
    @Override
    public void execute() {
        oldUser = manager.getUser(id);
        User user = manager.getUser(id);
        if (user != null) {
            user.setName(newName);
            user.setEmail(newEmail);
            user.setAge(newAge);
            manager.notifyObservers("UPDATE", user);
        }
    }
    
    @Override
    public void undo() {
        User user = manager.getUser(id);
        if (user != null && oldUser != null) {
            user.setName(oldUser.getName());
            user.setEmail(oldUser.getEmail());
            user.setAge(oldUser.getAge());
            manager.notifyObservers("UNDO_UPDATE", user);
        }
    }
}

class DeleteUserCommand implements Command {
    private UserManager manager;
    private String id;
    private User deletedUser;
    
    public DeleteUserCommand(UserManager manager, String id) {
        this.manager = manager;
        this.id = id;
    }
    
    @Override
    public void execute() {
        deletedUser = manager.getUser(id);
        manager.removeUserDirectly(id);
        manager.notifyObservers("DELETE", deletedUser);
    }
    
    @Override
    public void undo() {
        if (deletedUser != null) {
            manager.addUserDirectly(deletedUser);
            manager.notifyObservers("UNDO_DELETE", deletedUser);
        }
    }
}

interface UserFilterStrategy {
    boolean test(User user);
}

class AgeFilterStrategy implements UserFilterStrategy {
    private int minAge;
    private int maxAge;
    
    public AgeFilterStrategy(int minAge, int maxAge) {
        this.minAge = minAge;
        this.maxAge = maxAge;
    }
    
    @Override
    public boolean test(User user) {
        return user.getAge() >= minAge && user.getAge() <= maxAge;
    }
}

class NameFilterStrategy implements UserFilterStrategy {
    private String keyword;
    
    public NameFilterStrategy(String keyword) {
        this.keyword = keyword.toLowerCase();
    }
    
    @Override
    public boolean test(User user) {
        return user.getName().toLowerCase().contains(keyword);
    }
}

class EmailFilterStrategy implements UserFilterStrategy {
    private String domain;
    
    public EmailFilterStrategy(String domain) {
        this.domain = domain.toLowerCase();
    }
    
    @Override
    public boolean test(User user) {
        return user.getEmail().toLowerCase().contains(domain);
    }
}

interface UserObserver {
    void onUserChanged(String action, User user);
}

class LoggingObserver implements UserObserver {
    @Override
    public void onUserChanged(String action, User user) {
        System.out.println("[日志] 操作: " + action + ", 用户: " + (user != null ? user.getName() : "null"));
    }
}

class StatisticsObserver implements UserObserver {
    private int createCount = 0;
    private int updateCount = 0;
    private int deleteCount = 0;
    
    @Override
    public void onUserChanged(String action, User user) {
        if (action.equals("CREATE")) createCount++;
        else if (action.equals("UPDATE")) updateCount++;
        else if (action.equals("DELETE")) deleteCount++;
    }
    
    public void printStatistics() {
        System.out.println("[统计] 创建: " + createCount + ", 更新: " + updateCount + ", 删除: " + deleteCount);
    }
}

class UserManager {
    private static UserManager instance;
    private Map<String, User> users = new HashMap<>();
    private List<Command> commandHistory = new ArrayList<>();
    private List<UserObserver> observers = new ArrayList<>();
    
    private UserManager() {
        addObserver(new LoggingObserver());
        addObserver(new StatisticsObserver());
    }
    
    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }
    
    public void executeCommand(Command command) {
        command.execute();
        commandHistory.add(command);
        if (commandHistory.size() > 10) {
            commandHistory.remove(0);
        }
    }
    
    public boolean undo() {
        if (commandHistory.isEmpty()) {
            return false;
        }
        Command command = commandHistory.remove(commandHistory.size() - 1);
        command.undo();
        return true;
    }
    
    public void addUserDirectly(User user) {
        users.put(user.getId(), user);
    }
    
    public void removeUserDirectly(String id) {
        users.remove(id);
    }
    
    public User getUser(String id) {
        return users.get(id);
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public List<User> filterUsers(UserFilterStrategy strategy) {
        List<User> result = new ArrayList<>();
        for (User user : users.values()) {
            if (strategy.test(user)) {
                result.add(user);
            }
        }
        return result;
    }
    
    public void addObserver(UserObserver observer) {
        observers.add(observer);
    }
    
    public void notifyObservers(String action, User user) {
        for (UserObserver observer : observers) {
            observer.onUserChanged(action, user);
        }
    }
}
