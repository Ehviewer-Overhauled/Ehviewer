extern crate android_logger;
extern crate apply;
extern crate jni;
extern crate log;
extern crate tl;

use std::ffi::c_void;

use android_logger::Config;
use apply::Also;
use jni::JNIEnv;
use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::{JavaVM, jint, jintArray, JNI_VERSION_1_6};
use tl::{Parser, VDom};

fn parse_jni_string<F, R>(env: &mut JNIEnv, str: &JString, mut f: F) -> Option<R>
    where F: FnMut(&VDom, &Parser, &JNIEnv) -> Option<R> {
    let html = env.get_string(str).ok()?;
    let dom = tl::parse(html.to_str().ok()?, tl::ParserOptions::default()).ok()?;
    let parser = dom.parser();
    Some(f(&dom, parser, env)?)
}

#[no_mangle]
pub extern "system" fn Java_com_hippo_ehviewer_client_parser_HomeParserKt_parseLimit(mut env: JNIEnv, _class: JClass, input: JString) -> jintArray {
    let vec = parse_jni_string(&mut env, &input, |dom, parser, _env| {
        let iter = dom.query_selector("strong")?;
        let vec: Vec<i32> = iter.filter_map(|e| Some(e.get(parser)?.inner_text(parser).parse::<i32>().ok()?)).collect();
        if vec.len() == 3 { Some(vec) } else { None }
    }).unwrap_or(vec![]);
    env.new_int_array(3).unwrap().also(|it| env.set_int_array_region(&it, 0, &vec).unwrap()).into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_hippo_ehviewer_client_parser_FavoritesParserKt_parseFav(mut env: JNIEnv, _class: JClass, input: JString, str: JObjectArray) -> jintArray {
    let vec = parse_jni_string(&mut env, &input, |dom, parser, env| {
        let fp = dom.get_elements_by_class_name("fp");
        let vec: Vec<i32> = fp.enumerate().filter_map(|(i, e)| {
            if i == 10 { return None; }
            let top = e.get(parser)?.children()?;
            let children = top.top();
            let cat = children[5].get(parser)?.inner_text(parser);
            env.set_object_array_element(&str, i as i32, env.new_string(cat.trim()).ok()?).ok()?;
            Some(children[1].get(parser)?.inner_text(parser).parse::<i32>().ok()?)
        }).collect();
        if vec.len() == 10 { Some(vec) } else { None }
    }).unwrap_or(vec![]);
    env.new_int_array(10).unwrap().also(|it| env.set_int_array_region(&it, 0, &vec).unwrap()).into_raw()
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default());
    JNI_VERSION_1_6
}
