package com.apm.collects;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;

import com.apm.init.AbstractCollects;
import com.apm.init.AgentLoader;
import com.apm.init.Collect;
import com.apm.init.NotProguard;

import javassist.CtClass;
import javassist.CtMethod;

/**
 * jdbc 数据采集
 */
@NotProguard
public class JdbcCommonCollects extends AbstractCollects implements Collect {
    @NotProguard
    public static final JdbcCommonCollects INSTANCE = new JdbcCommonCollects();

    private final static String[] connection_agent_methods = new String[]{"prepareStatement"};
    private final static String[] prepared_statement_methods = new String[]{"execute", "executeUpdate", "executeQuery"};
    private static final String beginSrc;
    private static final String endSrc;
    private static final String errorSrc;

    static {
        StringBuilder sbuilder = new StringBuilder();
        // connect
        beginSrc = "com.apm.collects.JdbcCommonCollects inst = com.apm.collects.JdbcCommonCollects.INSTANCE;";
        errorSrc = "inst.error(null,e);";
        endSrc = "result=inst.proxyConnection((java.sql.Connection)result);";
    }

    /**
     * 判断是否是监控目标类
     */
    public boolean isTarget(String className, ClassLoader loader, CtClass ctclass) {
        if (className.equals("com.mysql.jdbc.NonRegisteringDriver")) {
            System.out.println("JdbcCommonCollects--1-mysql---NonRegisteringDriver");
            return true;
        }else if (className.equals("com.alibaba.druid.pool.DruidDataSource")) {
            System.out.println("JdbcCommonCollects--2-mysql---NonRegisteringDriver");
            return true;
        }else if (className.equals("com.mysql.cj.jdbc.NonRegisteringDriver")) {
            System.out.println("JdbcCommonCollects--3-mysql---NonRegisteringDriver");
        	return true;
        }
        return false;
    }
    @NotProguard
    @Override
    public Statistics begin(String className, String method) {
        JdbcStatistics jdbcStat = new JdbcStatistics(super.begin(className, method));
        jdbcStat.logType = "sql";
        return jdbcStat;
    }

    @NotProguard
    @Override
    public void end(Statistics stat) {
        JdbcStatistics jdbcStat= (JdbcStatistics) stat;
        if (jdbcStat.jdbcUrl != null) {
            jdbcStat.databaseName = getDbName(jdbcStat.jdbcUrl);
        }
        super.end(stat);
    }

    @Override
    public void sendStatistics(Statistics stat) {
        sendStatisticByHttp(stat, "sqlLog");
    }

    @NotProguard
    public Connection proxyConnection(final Connection connection) {
        Object c = Proxy.newProxyInstance(JdbcCommonCollects.class.getClassLoader()
                , new Class[]{Connection.class}, new ConnectionHandler(connection));
        return (Connection) c;
    }


    public PreparedStatement proxyPreparedStatement(final PreparedStatement statement, JdbcStatistics jdbcStat) {
        Object c = Proxy.newProxyInstance(JdbcCommonCollects.class.getClassLoader()
                , new Class[]{PreparedStatement.class}, new PreparedStatementHandler(statement, jdbcStat));
        return (PreparedStatement) c;
    }
    
    /**
     * 对目标类进行转 也就是采集（监控代码的插入）
     */
    public byte[] transform(ClassLoader loader, String className, byte[] classfileBuffer, CtClass ctclass) throws Exception {
        AgentLoader byteLoade = new AgentLoader(className, loader, ctclass);
        CtMethod connectMethod = ctclass.getMethod("connect", "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");
//      connectMethod.getMethodInfo().getDescriptor();
        AgentLoader.MethodSrcBuild build = new AgentLoader.MethodSrcBuild();
        build.setBeginSrc(beginSrc);
        build.setErrorSrc(errorSrc);
        build.setEndSrc(endSrc);
        byteLoade.updateMethod(connectMethod, build);
        return byteLoade.toBytecote();
    }


    /**
     * connection 代理处理
     */
    public class ConnectionHandler implements InvocationHandler {
        private final Connection connection;

        private ConnectionHandler(Connection connection) {
            this.connection = connection;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean isTargetMethod = false;
            PreparedStatement statement = null;
//            for (String agentm : connection_agent_methods) {
//                if (agentm.equals(method.getName())) {
//                    isTargetMethod = true;
//                    break;
//                }
//            }
            if (Arrays.<String>stream(JdbcCommonCollects.connection_agent_methods).anyMatch(s -> s.equals(method.getName()))){
                isTargetMethod = true;
            }
            Object result = null;
            JdbcStatistics jdbcStat = null;
            try {
                if (isTargetMethod) { // 获取PreparedStatement 开始统计
                    jdbcStat = (JdbcStatistics) JdbcCommonCollects.this.begin(null, null);
                    jdbcStat.jdbcUrl = connection.getMetaData().getURL();
                    jdbcStat.sql = (String) args[0];
                }
                result = method.invoke(connection, args);
                // 代理 PreparedStatement
                if (isTargetMethod && result instanceof PreparedStatement) {
                    PreparedStatement ps = (PreparedStatement) result;
                    result = proxyPreparedStatement(ps, jdbcStat);
                }
            } catch (Throwable e) {
                JdbcCommonCollects.this.error(jdbcStat, e);
                JdbcCommonCollects.this.end(jdbcStat);
                throw e;
            }
            return result;
        }
    }

    /**
     * PreparedStatement 代理处理
     */
    public class PreparedStatementHandler implements InvocationHandler {
        private final PreparedStatement statement;
        private final JdbcStatistics jdbcStat;

        public PreparedStatementHandler(PreparedStatement statement, JdbcStatistics jdbcStat) {
            this.statement = statement;
            this.jdbcStat = jdbcStat;
        }
//        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//            if (Arrays.<String>stream(JdbcCommonCollects.prepared_statement_methods).anyMatch(s -> s.equals(method.getName())))
//                JdbcCommonCollects.printSql(this.statement);
//            return method.invoke(this.statement, args);
//        }
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean isTargetMethod = false;
//            for (String agentm : prepared_statement_methods) {
//                if (agentm.equals(method.getName())) {
//                    isTargetMethod = true;
//
//                }
//            }
            if (Arrays.<String>stream(JdbcCommonCollects.prepared_statement_methods).anyMatch(s -> s.equals(method.getName()))){
                isTargetMethod = true;
            }
            Object result = null;
            try {
                result = method.invoke(statement, args);
            } catch (Throwable e) {
                if (isTargetMethod) {
                    JdbcCommonCollects.this.error(jdbcStat, e);
                }
                throw e;
            } finally {
                if (isTargetMethod) {
                    String printSql = JdbcCommonCollects.printSql(this.statement);
                    jdbcStat.parameterSql = printSql;
                    JdbcCommonCollects.this.end(jdbcStat);
                }
            }
            return result;
        }
    }

    @NotProguard
    public static class JdbcStatistics extends Statistics {
        // jdbc url
        public String jdbcUrl;
        // sql 语句
        public String sql;
        //带参数
        public String parameterSql;
        // 数据库名称
        public String databaseName;

        public JdbcStatistics() {

        }
        public JdbcStatistics(Statistics stat) {
            super(stat);
        }
    }

    /**
     * 获取数据库名称
     * @param url
     * @return
     */
    private static String getDbName(String url) {
        int index = url.indexOf("?"); //$NON-NLS-1$
        if (index != -1) {
            String paramString = url.substring(index + 1, url.length());
            url = url.substring(0, index);
        }
        String dbName = url.substring(url.lastIndexOf("/") + 1);
        return dbName;
    }
    public static String printSql(Statement sta) {
        String sql = sta.toString();
        String prefix = "com.mysql.cj.jdbc.ClientPreparedStatement:";
        if (sql.startsWith(prefix))
            sql = sql.split("ClientPreparedStatement:")[1];
//        System.out.println("\r\n--------------------------------------------\r\n" + sql + "\r\n--------------------------------------------");
        return sql;

    }
}
