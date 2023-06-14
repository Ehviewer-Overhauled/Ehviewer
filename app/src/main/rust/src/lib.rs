extern crate android_logger;
extern crate apply;
extern crate jni;
extern crate log;
extern crate once_cell;
extern crate regex;
extern crate tl;

use std::ffi::c_void;

use android_logger::Config;
use apply::Also;
use jni::JNIEnv;
use jni::objects::{JClass, JList, JObject, JObjectArray, JString};
use jni::sys::{JavaVM, jint, jintArray, JNI_VERSION_1_6};
use once_cell::sync::Lazy;
use regex::Regex;
use tl::{Parser, VDom};

static TORRENT_REGEX: Lazy<Regex> = Lazy::new(|| { Regex::new("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9.]+ [KMGT]B)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>").unwrap() });

fn parse_jni_string<F, R>(env: &mut JNIEnv, str: &JString, mut f: F) -> Option<R>
    where F: FnMut(&VDom, &Parser, &mut JNIEnv) -> Option<R> {
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
pub extern "system" fn Java_com_hippo_ehviewer_client_parser_TorrentParserKt_parseTorrent(mut env: JNIEnv, _class: JClass, input: JString, list: JObject) {
    let list = JList::from_env(&mut env, &list).unwrap();
    let string = env.find_class("java/lang/String").unwrap();
    parse_jni_string(&mut env, &input, |dom, parser, env| {
        let _: Vec<i32> = dom.query_selector("table")?.filter_map(|e| {
            let array = env.new_object_array(7, &string, JObject::null()).ok()?;
            let html = e.get(parser)?.inner_html(parser);
            let grp = TORRENT_REGEX.captures(&html)?;
            let vec = vec![&grp[1], &grp[2], &grp[3], &grp[4], &grp[5], &grp[7], &grp[8]];
            vec.iter().enumerate().for_each(|(i, &n)| { env.set_object_array_element(&array, i as i32, env.new_string(n).unwrap()).unwrap(); });
            list.add(env, &array).ok()?;
            None
        }).collect();
        Some(0)
    });
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default());
    JNI_VERSION_1_6
}
