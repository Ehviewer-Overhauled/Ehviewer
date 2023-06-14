extern crate jni;
extern crate tl;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jintArray;
use tl::{Parser, VDom};

fn parse_jni_string<F, R>(env: &mut JNIEnv, str: &JString, mut f: F) -> Option<R>
    where F: FnMut(&VDom, &Parser) -> Option<R> {
    let html = env.get_string(str).ok()?;
    let dom = tl::parse(html.to_str().ok()?, tl::ParserOptions::default()).ok()?;
    let parser = dom.parser();
    Some(f(&dom, parser)?)
}

#[no_mangle]
pub extern "system" fn Java_com_hippo_ehviewer_client_parser_HomeParserKt_parseLimit<'local>(mut env: JNIEnv<'local>, _class: JClass<'local>, input: JString<'local>) -> jintArray {
    let vec = parse_jni_string(&mut env, &input, |dom, parser| {
        let iter = dom.query_selector("strong")?;
        let vec: Vec<i32> = iter.filter_map(|e| Some(e.get(parser)?.inner_text(parser).parse::<i32>().ok()?)).collect();
        if vec.len() == 3 { Some(vec) } else { None }
    }).unwrap_or(vec![-1, -1, -1]);
    let jir = env.new_int_array(3).unwrap();
    env.set_int_array_region(&jir, 0, &vec).unwrap();
    jir.into_raw()
}
