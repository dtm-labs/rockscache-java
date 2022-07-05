[English](https://github.com/dtm-labs/rockscache-java) | 简体中文。

# rockscache-java

一个 Redis缓存库，确保redis和DB的的一致性，支持最终一致和强一致两种模式。

本库是go语言实现的[rockscache](https://github.com/dtm-labs/rockscache)的Java移植版，原项目文档中对工作原理有详尽的阐述。因此，本文不重复原理阐述，仅介绍使用方法。

## 如何运行example

1. git clone本项目，使用IDE打开其中example目录所代表的工程，等待gradle下载所有依赖。

2. 使用Docker在本机构建一个redis服务，端口保持默认的6379

   > 如果想基于现有的redis运行此demo，请修改[application.yml](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/application.yml)中sprin.redis相关配置。

3. 使用Docker在本机构建一个mysql服务，端口保持默认的3306

   > 如果想基于现有的mysql运行此demo，请修改[application.yml](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/application.yml)中sprin.datasource相关配置。
   
4. 在mysql中建立表并初始化数据

   连接上一步建立的mysql，运行[data.sql](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/resources/data.sql)中所有代码

5. 启动example项目

   直接用IDE运行[ExampleApplication](https://github.com/dtm-labs/rockscache-java/blob/main/example/src/main/java/io/github/dtmlabs/rcokscache/example/ExampleApplication.java)类即可

6. 访问http://localhost:8080/ui

## 如何构建自己的项目

### 添加依赖

先建立一个spring-boot项目。

- 对于使用gradle的用户，请添加依赖
  ```
  implementation 'io.github.dtm-labs:rockscache-spring-boot:0.0.4'
  ```

- 对于使用maven的用户，请添加依赖
  ```
  <dependency>
    <groupId>io.github.dtm-labs</groupId>
    <artifactId>rockscache-spring-boot</artifactId>
    <version>0.0.4</version>
  </dependency>
  ```
  
### 在application.properties或application.yml中定义spring.redis相关配置

### 定义程序需要的所有缓存名

```java
public class CacheNames {
    private CachesNames() {}
    public static final String PRODUCT = "productCache";
    public static final String ORDER = "orderCache";
    public static final String USER = "userCache";
}
```

### 创建项目需要的所有缓存实例

```java
@Configuration
public class CacheConfig {

    private final CacheClinet cacheClient;

    private final ProductRepository productRepository;

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    public CacheConfig(

        // 只要依赖了rockscache-spring-boot
        // CacheClient对象就会被自动创建，
        // 简单地注入即可 
        CacheClient cacheClient,

        // 注入所有数据访问对象
        ProductRepository productRepository,
        OrderRepository orderRepository,
        UserRepository userRepository

    ) {
        this.cacheClient = cacheClient;

        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Bean(CacheNames.PRODUCT)
    public Cache<Long, Product> productCache() {
        return cacheClient
                .newCacheBuilder(
                        "product-", // 1
                        Long.class, // 2
                        Product.class //3
                )
                .setJavaLoader( //4
                        Product::getId, // 5
                        productRepository::findAllById //6
                )
                .build();
    }

    @Bean(CacheNames.ORDER)
    public Cache<Long, Order> orderCache() {
        return cacheClient
                .newCacheBuilder(
                        "order-", // 1
                        Long.class, // 2
                        Order.class // 3
                )
                .setJavaLoader( // 4
                        Order::getId, // 5
                        orderRepository::findAllById // 6
                )
                .build();
    }

    @Bean(CacheNames.USER)
    public Cache<Long, User> userCache() {
        return cacheClient
                .newCacheBuilder(
                        "user-", // 1
                        Long.class, // 2
                        User.class // 3
                )
                .setJavaLoader( // 4
                        User::getId, // 5
                        userRepository::findAllById // 6
                )
                .build();
    }

} 
```

上面的代码创建了程序所需要的所有缓存实例，对于上述代码中的注释，解释如下

1. 设置缓存中键的前缀

   redis的键是字符串，由该前缀和用户的业务键拼接而成。

   例如，上例中，订单缓冲的前缀为`order-`，那么id为`1`的订单，在redis中对应的键就是`order-1`

2. 键类型

3. 值类型

   默认情况下，redis中的值，为该类型的Java对象进行jackson序列化的结果。

4. setJavaLoader方法表示，如果查看的数据在reids中不存在，该如何从数据库中加载。

   > 这里的`setJavaLoader`是针对Java开发者的，如果是kotlin开发者，请调用`setKtLoader`。

5. 如果数据访问行为返回`List<V>`，而非`Map<K, V>`，这个Lambda表达式表示如何从值对象获得键。

   > 如果数据访问行为返回`Map<K, V>`; 需调用`setJavaLoader`方法的其他重载形式，而其他重载形式无此参数。对于某些根本没有id的对象，这些重载版会很有用。

6. 一个Lambda表达式，表示数据访问行为

   > 在本文所讨论用法所涉及的重载版本中，这个lambda的签名是`(List<K>) -> List<V>`。在使用spring-data的前提下，这个形式的数据查询总会被自动生成，无需开发。

### 注入缓存

```java
@Service
public class OrderService {
    
    private Cache<Long, Order> orderCache;
    
    private OrderRepository orderRepository;

    public OrderSerivce(

        // 由于缓存对象由很多个，请明确指定@Qualifier
        @Qualifier(CacheNames.ORDER) Cache<Long> orderCache,

        OrderRepository orderRepository
    )
    
    public Order findOrder(long id) {
        return orderCache.get(id);
    }

    public void saveOrder(Order order) {
        
        // 先修改数据库
        employeeRepository.save(order);

        /* 在数据库修改完成后，
         * 将被修改的redis数据标记成已删除
         *
         * 这个例子仅仅是烦如何使用tagAsDeleted，
         * 不示范如何应对tagAsDeleted这个操作的异常的情况
         *（更极端的案例，tagAsDeleted之前程序就挂了，没执行）
         *
         * 在实际项目中，请使用你喜欢的任何
         * 可靠消息技术，保证tagAsDeleted一定会被执行。
         *
         * DTM二阶段消息就是一个理想的选择。
         */
        orderCache.tagAsDeleted(order.getId());
    }
}
```

### Cache接口的行为

```kotlin
interface Cache<K, V> {

    fun toCache(consistency: Consistency): Cache<K, V>

    fun fetch(key: K): V? =
        fetchAll(setOf(key))[key]

    fun fetch(key: K, consistency: Consistency): V? =
        fetchAll(setOf(key), consistency)[key]

    fun fetchAll(keys: Collection<K>): Map<K, V>

    fun fetchAll(keys: Collection<K>, consistency: Consistency): Map<K, V>

    fun tagAsDeleted(key: K) {
        tagAllAsDeleted(setOf(key))
    }

    fun tagAllAsDeleted(keys: Collection<K>)

    fun lockOperator(key: K, lockId: String): LockOperator<K> =
        lockAllOperator(setOf(key), lockId)

    fun lockAllOperator(keys: Collection<K>, lockId: String): LockOperator<K>
}
```

- `toCache`：对于`fetch(K)`和`fetchAll(Collection<K>)`这两个没有明确指定数据型一致性要求，即`Consistency`。默认情况下，会采用缓存对象默认的一致性。

  > 注意：
  > 
  > 此方法并不会改表当前Cache的默认Consistency，而是创建一个新的Cache，除了默认的Consistency不同外，新旧Cache的功能没有任何差异。

- `fetch(K)`: 以当前缓存对象默认的一致性要求，查询一个数据

- `fetch(K, Consistency)`: 以用户制定的的一致性要求，查询一个数据

- `fetchAll(Collection<K>)`: 以当前缓存对象默认的一致性要求，查询多个数据

- `fetchAll(Collection<K>, Consistency)`: 以用户制定的的一致性要求，查询多个数据

- `tagAsDeleted`: 将一个数据标记成已删除

- `tagAllAsDeleted`: 将多个数据标记成已删除

- lockOperator(K, String): 制定一个随机的lockId，获取一个数据的锁操作对象
  
  > 注意
  > 
  > 返回的对象支持两个方法：`lock`和`unlock`
  > 当前对象返回是，并未上锁，必须调用返回对象的`lock`方法才会上锁
  > 返回对象的`lock`需要制定最大等待时间。如在规定时间内不能抢到锁，抛出异常
  > 由于分布式通信的不可靠性，一旦调用了返回对象的`lock`，无论成败，都需要调用其`unlock`，而且要使用可靠消息来保证这点。

- lockAllOperator(Collection<K>, String): 制定一个随机的lockId，获取多个数据的锁操作对象
  
  > 注意事项同lockOperator

### 一致性要求

```kotlin
enum class Consistency {
    EVENTUAL,
    STRONG,
    TRY_STRONG
}
```

- EVENTUAL: 最终一致。

    只要缓存中存在数据，无论是否一致，都立即返回。

    如果此举导致某个不一致的数据被返回了，系统会让后续请求尽快地能拿到一致的数据。

- STRONG: 强一致

    除非缓存中现有数据是一致的，否则，以等待为代价，保证返回一致的数据。

    此选项不适合高并发场合。

- TRY_STRONG: 尝试强一致。

    除非缓存中现有数据是一致的，否则，抛出异常：`io.github.dtm.cache.DirtyCacheException`

    此异常可以让界面显示：“数据展示不可用，请稍后重试”，从而引导界面的到一致的数据。


Consistency采用多级继承覆盖的配置方法

1. 全局的`CacheClient`对象的consistency属性，由spring boot配置文件`application.yml`或`applicaiton.properties`中的配置`rockscache.consistency`决定

   > 如果配置文件未指定其值，此配置默认为EVENTUAL

2. 从`CacheClient`创建`Cache`时，可以覆盖步骤1中的设置

   ```java
   @Bean(CacheNames.ORDER)
   public Cache<Long, Order> orderCache() {
       return cacheClient
               .newCacheBuilder(
                       "order-", 
                       Long.class, 
                       Order.class 
               )
               .setConsistency(Consistency.STRONG) // 覆盖全局配置
               .setJavaLoader( 
                       Order::getId, 
                       orderRepository::findAllById 
               )
               .build();
    }
   ```

   > 如果缓存级的配未指定，继承全局配置。

3. 构建新的Cache，覆盖旧Cache

   ```
   Cache<K, V> newCache = oldCache.toCache(Consistency.STRONG);
   ```

4. 查询时明确指定Consistency

   `fetch(K, Consistency)`和`fetchAll(Collection<K>, Consistency)`这两个数据查询的重载版本，明确指定Consistency，无视任何级别的配置。

### Spring配置

|配置项｜描述｜类型｜默认值｜
|-----|----|----|------|
|rockscache.globalKeyPrefix|如果不同微服务共享redis(虽然不推荐，但是有可能)，为了防止不同微服务的缓存彼此打架而引入的名字空间。加入指定为`my-service-`，这时reids中key的名称为`my-service-order-1`｜String｜无｜
|rockscache.delay|被`tagAsDeleted`/`tagAllAsDeleted`的数据在redis中的过期时间｜Duration｜PT10S｜
|rockscache.emptyExpire|null数据在reids中的过期时间｜Duration｜PT60S｜
|rockscache.lockExpire|解决数据一致性的锁的时间｜Duration｜PT3S｜
|rockscache.lockSleep|无法抢夺锁的重试时间间隔|Duration|PT0.1S|
|rockscache.waitReplicas|如果大于0，表示`tagAsDeleted`/`tagAllAsDeleted`需要等待的redis从服务器同步数量|int|0|
|rockscache.waitReplicasTimeout|和waitReplicas配合使用，等待时间|Duration|PT3S|
|rockscache.isDisableCacheRead|是否禁用缓存读取能力，如果禁用，不再使用缓存而直接读取数据库|boolean|false|
|rockscache.isDisableCacheDelete|是否禁用缓存删除能力，如果禁用，`tagAsDeleted`/`tagAllAsDeleted`操作内部无任何效果|boolean|false|
|rockscache.consistency|全局的默认一致性要求|Consistency|EVENTAL|
|rockscache.batchSize|批量操作上限，`fetchAll`和`tagAllAsDeleted`指定数据过多时，按此限制分批操作；`lockOperatorAll`指定数据过多则抛出异常|int|128|


