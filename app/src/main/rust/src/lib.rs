extern crate jni;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jintArray;

fn parse_limit(body: &str) -> Option<Vec<i32>> {
    let dom = tl::parse(body, tl::ParserOptions::default()).ok()?;
    let parser = dom.parser();
    let home_box = dom.query_selector("div.homebox")?.next()?.get(parser)?;
    let html2 = home_box.inner_html(parser);
    let dom2 = tl::parse(&*html2, tl::ParserOptions::default()).ok()?;
    let parser = dom2.parser();
    let iter = dom2.query_selector("strong")?;
    let vec: Vec<i32> = iter.filter_map(|e| Some(e.get(parser)?.inner_text(parser).parse::<i32>().ok()?)).collect();
    Some(vec)
}

#[no_mangle]
pub extern "system" fn Java_com_hippo_ehviewer_client_parser_HomeParserKt_parseLimit<'local>(mut env: JNIEnv<'local>, _class: JClass<'local>, input: JString<'local>) -> jintArray {
    let html = env.get_string(input.as_ref()).unwrap();
    let vec = parse_limit(html.to_str().unwrap());
    let jir = env.new_int_array(3).unwrap();
    env.set_int_array_region(&jir, 0, vec.unwrap_or(vec![-1, -1, -1]).as_ref()).unwrap();
    jir.into_raw()
}
