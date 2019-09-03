import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;

import java.util.*;


public class demo {



    public static List getStmt(Node node){
        List stmts = new ArrayList();
        List childs = node.getChildNodes();
        boolean flag = true;//如果一个子节点都不是stmt，那么就添加它自己，否则向下一层
        for (Object child : childs) {
            if(child instanceof Statement){
                stmts.addAll(getStmt((Node)child));
                flag = false;

            }
            else if(child instanceof CatchClause){
                stmts.addAll(getStmt((Node)child));
                flag = false;

            }
        }
        if(flag)
        {stmts.add(node);}
        return stmts;
    }
    public  static List<MethodDeclaration>  findMethods(Node node){
        if(node == null)return null;
        List<MethodDeclaration> methods = new ArrayList<>();
        if(node instanceof MethodDeclaration){
            methods.add((MethodDeclaration) node);

        }
        else{
            List childs = node.getChildNodes();
            for (Object child : childs) {
                List ms = findMethods((Node) child);
                if(ms != null){methods.addAll(ms);}
            }
        }

        return methods;
    }

    public static List getVars(Node node){
        List results = new ArrayList();
        List childs = node.getChildNodes();
        for (Object child : childs) {
            if(child instanceof NameExpr){
                results.add(((NameExpr) child).getName().toString());
                results.addAll(getVars((Node)child));
            }
            else if(child instanceof VariableDeclarator){//加入对变量声明的处理
                results.add(((VariableDeclarator) child).getName().toString());
                results.addAll(getVars((Node)child));
            }
            else{
                results.addAll(getVars((Node)child));
            }
        }
        return results;
    }

    public static List getTypes(Node node){
        //记录变量声明时候=，某个变量的类型名
        List results = new ArrayList();
        List childs = node.getChildNodes();
        for (Object child : childs) {
            if(child instanceof VariableDeclarator){
                Map map = new HashMap();
                map.put("var",((VariableDeclarator) child).getName().toString());
                map.put("type",((VariableDeclarator) child).getType().asString());
                results.add(map);
                results.addAll(getTypes((Node)child));
            }
            else{
                results.addAll(getTypes((Node)child));
            }
        }
       return results;

    }

    public static List getFuncs(Node node){
        List results = new ArrayList();
        List childs = node.getChildNodes();
        for (Object child : childs) {
            if(child instanceof MethodCallExpr){
                results.add(((MethodCallExpr) child).getName().toString());
                results.addAll(getFuncs((Node)child));
            }
            else if(child instanceof ObjectCreationExpr){
                results.add(((ObjectCreationExpr) child).getType().getName().toString());
                results.addAll(getFuncs((Node)child));
            }
            else{
                results.addAll(getFuncs((Node)child));
            }
        }
        return results;
    }

    public static void stmtAnalysis(Node node){
        List vars = getVars(node);
        List funcs = getFuncs(node);
        System.out.println();
    }

    public static List getFields(Node node,String vname){
        List results = new ArrayList();
        List childs = node.getChildNodes();
        for (Object child : childs) {
            if(child instanceof FieldAccessExpr){
                String name = ((FieldAccessExpr) child).getScope().toString();
                if(name.equals(vname)){
                    results.add(((FieldAccessExpr) child).getName().toString());
                }

                results.addAll(getFields((Node)child,vname));
            }
            else{
                results.addAll(getFields((Node)child,vname));
            }
        }
        return results;
    }
    public static void getVarPool(Node stmt,List varpool,Map varCache){
        List vars = getVars(stmt);
        List funcs = getFuncs(stmt);
        List types = getTypes(stmt);
        HashSet h = new HashSet(vars);
        vars.clear();
        vars.addAll(h);

        HashSet h2 = new HashSet(funcs);
        funcs.clear();
        funcs.addAll(h2);


        for(Object v : vars){
            List fields = getFields(stmt,(String)v);

            Map var = new HashMap();
            var.put("fields",fields);
            var.put("var",v);
            var.put("methods",funcs);
            var.put("vars",vars);
            var.put("types",types);
            List indexs = new ArrayList();
            int i = 0;

            for(Object v2 : vars){
                if(varCache.containsKey(v2)){
                    indexs.add(varCache.get(v2));

                }else
                {
                    indexs.add(varpool.size());
                }
            }

            varCache.put(v,varpool.size());


            var.put("indexs",indexs);



            varpool.add(var);
        }
    }

    public static String listToString(List list, char separator) {
        if(list.size() == 0){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(separator);
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    public static String stateprocess(Map m){
        //输入变量池的一个元素，整理成一个序列
        String results = "";
        List types = (List)m.get("types");
        if(types.size()!=0){
            for (Object type : types) {
                results = results+((Map)type).get("var")+" "+((Map)type).get("type")+" ";
            }
        }
        results = results + listToString((List)(m.get("vars")),' ');
        results = results + " " + listToString((List)(m.get("methods")),' ');
        results = results + " " + (String) (m.get("var"));
        results = results + " " + listToString((List)(m.get("fields")),' ');

        return results;
    }

    public static List mainproces(List varpool){
        Map cache = new HashMap();
        List resultlist = new ArrayList();
        for(int i = varpool.size()-1;i>=0;i--){
            if(cache.containsKey(i))continue;
            resultlist.add(seqprocess(varpool,i,cache));

        }
        System.out.println();
        return resultlist;

    }
    public static List seqprocess(List varpool,int thisindex,Map cache){
        //输入varpool,根据index整理出关于某个变量的整个序列
        cache.put(thisindex,0);
        Map item = (Map)varpool.get(thisindex);
        List results = new ArrayList();
        String result = stateprocess(item);

        List indexs = (List)item.get("indexs");
        for (Object index : indexs) {
            if ((int)index == thisindex)continue;
            else{
                if(results.size()!=0){
                    results.addAll(seqprocess(varpool,(int)index,cache));

                    String last = (String)results.get(results.size()-1);
                    if(last.equals(result)){continue;}
                    else{
                        results.add(result);
                    }
                }
                else
                {
                    results.addAll(seqprocess(varpool,(int)index,cache));
                    results.add(result);
                }
//                results = results + ";" +seqprocess(varpool,(int)index,cache);
            }
        }
        return results;
    }

    public static void main(String[] args) {
        File file = new File("C:\\Users\\Bohong Liu\\Documents\\lab\\dataflow\\src\\testdata");
        String code = FileIO.readStringFromFile(file.getAbsolutePath());
        CompilationUnit compilationUnit = JavaParser.parse(code);
        List methods = findMethods(compilationUnit);
        for (Object method : methods) {
            //做一个变量池
            List varpool = new ArrayList();
            Map varCache = new HashMap();


            List stmts = getStmt((Node)method);

            for (Object stmt : stmts) {

                getVarPool((Node)stmt,varpool,varCache);


                stmtAnalysis((Node)stmt);

            }
           List result = mainproces(varpool);
            System.out.println();
        }

        System.out.println();
    }
}
