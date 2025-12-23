package com.zhenxi.Superappium.xpath.function;

import com.zhenxi.Superappium.SuperAppium;
import com.zhenxi.Superappium.xpath.function.axis.*;
import com.zhenxi.Superappium.xpath.function.axis.ParentFunction;
import com.zhenxi.Superappium.xpath.function.filter.*;
import com.zhenxi.Superappium.xpath.function.select.*;
import com.zhenxi.Superappium.xpath.function.select.SelfFunction;
import com.zhenxi.Superappium.xpath.function.select.TextFunction;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class FunctionEnv {
    private static Map<String, SelectFunction> selectFunctions = new HashMap<>();
    private static Map<String, FilterFunction> filterFunctions = new HashMap<>();
    private static Map<String, AxisFunction> axisFunctions = new HashMap<>();

    static {
        registerAllSelectFunctions();
        registerAllFilterFunctions();
        registerAllAxisFunctions();

    }

    public static SelectFunction getSelectFunction(String functionName) {
        return selectFunctions.get(functionName);
    }

    public static FilterFunction getFilterFunction(String functionName) {
        return filterFunctions.get(functionName);
    }

    public static AxisFunction getAxisFunction(String functionName) {
        return axisFunctions.get(functionName);
    }

    public synchronized static void registerSelectFunction(SelectFunction selectFunction) {
        if (selectFunctions.containsKey(selectFunction.getName())) {
            Log.w(SuperAppium.TAG, "register a duplicate  select function " + selectFunction.getName());
        }
        selectFunctions.put(selectFunction.getName(), selectFunction);
    }

    public synchronized static void registerFilterFunction(FilterFunction filterFunction) {
        if (filterFunctions.containsKey(filterFunction.getName())) {
            Log.w(SuperAppium.TAG, "register a duplicate  filter function " + filterFunction.getName());
        }
        filterFunctions.put(filterFunction.getName(), filterFunction);
    }

    public synchronized static void registerAxisFunciton(AxisFunction axisFunction) {
        if (axisFunctions.containsKey(axisFunction.getName())) {
            Log.w(SuperAppium.TAG, "register a duplicate  axis function " + axisFunction.getName());
        }
        axisFunctions.put(axisFunction.getName(), axisFunction);
    }

    private static void registerAllSelectFunctions() {
        registerSelectFunction(new AttrFunction());
        registerSelectFunction(new SelfFunction());
        registerSelectFunction(new TagSelectFunction());
        registerSelectFunction(new TextFunction());
    }

    private static void registerAllFilterFunctions() {
        registerFilterFunction(new AbsFunction());
        registerFilterFunction(new BooleanFunction());
        registerFilterFunction(new ConcatFunction());
        registerFilterFunction(new ContainsFunction());
        registerFilterFunction(new FalseFunction());
        registerFilterFunction(new FirstFunction());
        registerFilterFunction(new LastFunction());
        registerFilterFunction(new LowerCaseFunction());
        registerFilterFunction(new MatchesFunction());
        registerFilterFunction(new NameFunction());
        registerFilterFunction(new NotFunction());
        registerFilterFunction(new NullToDefaultFunction());
        registerFilterFunction(new PositionFunction());
        registerFilterFunction(new PredicateFunction());
        registerFilterFunction(new RootFunction());
        registerFilterFunction(new StringFunction());
        registerFilterFunction(new StringLengthFunction());
        registerFilterFunction(new SubstringFunction());
        registerFilterFunction(new com.zhenxi.Superappium.xpath.function.filter.TextFunction());
        registerFilterFunction(new ToDoubleFunction());
        registerFilterFunction(new ToIntFunction());
        registerFilterFunction(new TrueFunction());
        registerFilterFunction(new TryExeptionFunction());
        registerFilterFunction(new UpperCaseFunction());
        registerFilterFunction(new SipSoupPredicteJudgeFunction());
    }

    private static void registerAllAxisFunctions() {
        registerAxisFunciton(new AncestorFunction());
        registerAxisFunciton(new AncestorOrSelfFunction());
        registerAxisFunciton(new ChildFunction());
        registerAxisFunciton(new DescendantFunction());
        registerAxisFunciton(new DescendantOrSelfFunction());
        registerAxisFunciton(new FollowingSiblingFunction());
        registerAxisFunciton(new FollowingSiblingOneFunction());
        registerAxisFunciton(new ParentFunction());
        registerAxisFunciton(new PrecedingSiblingFunction());
        registerAxisFunciton(new com.zhenxi.Superappium.xpath.function.axis.SelfFunction());
        registerAxisFunciton(new SiblingFunction());
    }


}
