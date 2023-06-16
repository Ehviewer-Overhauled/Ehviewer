mod parser;

extern crate android_logger;
extern crate apply;
extern crate catch_panic;
extern crate html_escape;
extern crate jni_fn;
extern crate jnix;
extern crate jnix_macros;
extern crate log;
extern crate once_cell;
extern crate regex;
extern crate tl;

use android_logger::Config;
use apply::Also;
use catch_panic::catch_panic;
use html_escape::decode_html_entities;
use jni_fn::jni_fn;
use jnix::jni::objects::{JClass, JString};
use jnix::jni::sys::{jint, jintArray, jobjectArray, JavaVM, JNI_VERSION_1_6};
use jnix::jni::JNIEnv;
use jnix::JnixEnv;
use log::LevelFilter;
use std::ffi::c_void;
use tl::{Node, NodeHandle, Parser, VDom};

#[macro_export]
macro_rules! regex {
    ($re:literal $(,)?) => {{
        static RE: once_cell::sync::OnceCell<regex::Regex> = once_cell::sync::OnceCell::new();
        RE.get_or_init(|| regex::Regex::new($re).unwrap())
    }};
}

const EHGT_PREFIX: &str = "https://ehgt.org/";
const EX_PREFIX: &str = "https://s.exhentai.org/";

trait Anon {
    fn get_first_element_by_class_name(&self, name: &str) -> Option<&Node>;
}

impl<'a> Anon for VDom<'a> {
    fn get_first_element_by_class_name(&self, name: &str) -> Option<&Node> {
        let handle = self.get_elements_by_class_name(name).next()?;
        Some(handle.get(self.parser())?)
    }
}

fn parse_jni_string<F, R>(env: &mut JnixEnv, str: &JString, mut f: F) -> Option<R>
where
    F: FnMut(&VDom, &Parser, &JnixEnv) -> Option<R>,
{
    let html = env.get_string(*str).ok()?;
    let dom = tl::parse(html.to_str().ok()?, tl::ParserOptions::default()).ok()?;
    let parser = dom.parser();
    Some(f(&dom, parser, env)?)
}

fn get_node_handle_attr<'a>(
    node: &NodeHandle,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    Some(get_node_attr(node.get(parser)?, attr)?)
}

fn get_node_attr<'a>(node: &'a Node<'_>, attr: &'a str) -> Option<&'a str> {
    let str = node.as_tag()?.attributes().get(attr)??.try_as_utf8_str()?;
    Some(str)
}

// Should not use it at too upper node, since it will do DFS?
fn query_childs_first_match_attr<'a>(
    node: &'a Node<'_>,
    parser: &'a Parser,
    attr: &'a str,
) -> Option<&'a str> {
    let selector = format!("[{}]", attr);
    let mut iter = node.as_tag()?.query_selector(parser, &selector)?;
    Some(get_node_handle_attr(&iter.next()?, parser, attr)?)
}

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.FavoritesParserKt")]
pub fn parseFav(env: JNIEnv, _class: JClass, input: JString, str: jobjectArray) -> jintArray {
    let mut env = JnixEnv { env };
    let vec = parse_jni_string(&mut env, &input, |dom, parser, env| {
        let vec: Vec<i32> = dom
            .get_elements_by_class_name("fp")
            .enumerate()
            .filter_map(|(i, e)| {
                if i == 10 {
                    return None;
                }
                let top = e.get(parser)?.children()?;
                let children = top.top();
                let cat = children[5].get(parser)?.inner_text(parser);
                let name = decode_html_entities(&cat);
                env.set_object_array_element(str, i as i32, env.new_string(name.trim()).ok()?)
                    .ok()?;
                Some(
                    children[1]
                        .get(parser)?
                        .inner_text(parser)
                        .parse::<i32>()
                        .ok()?,
                )
            })
            .collect();
        if vec.len() == 10 {
            Some(vec)
        } else {
            None
        }
    })
    .unwrap();
    env.new_int_array(10)
        .unwrap()
        .also(|it| env.set_int_array_region(*it, 0, &vec).unwrap())
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Trace));
    JNI_VERSION_1_6
}
