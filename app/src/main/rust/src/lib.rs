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
use jnix::jni::sys::{jint, jintArray, jobject, jobjectArray, JavaVM, JNI_VERSION_1_6};
use jnix::jni::JNIEnv;
use jnix::{IntoJava, JnixEnv};
use jnix_macros::IntoJava;
use log::{error, LevelFilter};
use std::borrow::Cow;
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

fn to_category_i32(category: &str) -> i32 {
    match category {
        "misc" => 0x1,
        "doujinshi" => 0x2,
        "manga" => 0x4,
        "artistcg" | "artist cg sets" | "artist cg" => 0x8,
        "gamecg" | "game cg sets" | "game cg" => 0x10,
        "imageset" | "image sets" | "image set" => 0x20,
        "cosplay" => 0x40,
        "asianporn" | "asian porn" => 0x80,
        "non-h" => 0x100,
        "western" => 0x200,
        "private" => 0x400,
        _ => 0x800,
    }
}

fn parse_rating(str: &str) -> f32 {
    let reg = regex!("\\d+px");
    let mut iter = reg.find_iter(str);
    match (iter.next(), iter.next()) {
        (Some(num1), Some(num2)) => {
            let num1 = num1.as_str().replace("px", "").parse().unwrap_or(-1);
            let num2 = num2.as_str().replace("px", "").parse().unwrap_or(-1);
            if num1 == -1 || num2 == -1 {
                return -1.0;
            }
            let rate = 5 - num1 / 16;
            if num2 == 21 {
                (rate - 1) as f32 + 0.5
            } else {
                rate as f32
            }
        }
        _ => -1.0,
    }
}

#[derive(Default, IntoJava)]
#[allow(non_snake_case)]
#[jnix(package = "com.hippo.ehviewer.client.data")]
pub struct BaseGalleryInfo {
    gid: i64,
    token: String,
    title: String,
    titleJpn: Option<String>,
    thumbKey: String,
    category: i32,
    posted: String,
    uploader: Option<String>,
    disowned: bool,
    rating: f32,
    rated: bool,
    simpleTags: Vec<String>,
    pages: i32,
    thumbWidth: i32,
    thumbHeight: i32,
    spanSize: i32,
    spanIndex: i32,
    spanGroupIndex: i32,
    simpleLanguage: String,
    favoriteSlot: i32,
    favoriteName: Option<String>,
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

fn get_node_attr<'a>(node: &NodeHandle, parser: &'a Parser, attr: &'a str) -> Option<&'a str> {
    let str = node
        .get(parser)?
        .as_tag()?
        .attributes()
        .get(attr)??
        .try_as_utf8_str()?;
    Some(str)
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
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.GalleryListParserKt")]
pub fn parseGalleryInfo(env: JNIEnv, _class: JClass, input: JString) -> jobject {
    let mut env = JnixEnv { env };
    parse_jni_string(&mut env, &input, |dom, parser, _env| {
        let title = match dom.get_first_element_by_class_name("glink") {
            None => "".to_string(),
            Some(glink) => glink.inner_text(parser).to_string(),
        };
        let thumb = match dom.query_selector("[data-src]")?.next() {
            None => match dom.query_selector("[src]")?.next() {
                None => "a",
                Some(thumb) => get_node_attr(&thumb, parser, "src")?,
            },
            Some(thumb) => get_node_attr(&thumb, parser, "data-src")?,
        }
        .to_string();
        let category = match dom.get_first_element_by_class_name("cn") {
            None => match dom.get_first_element_by_class_name("cs") {
                None => Cow::from("unknown"),
                Some(cs) => cs.inner_text(parser),
            },
            Some(cn) => cn.inner_text(parser),
        };
        let ir = dom.get_first_element_by_class_name("ir")?;
        let rating = ir.as_tag()?.attributes().get("style")??.try_as_utf8_str()?;
        error!("{}", category);
        Some(BaseGalleryInfo {
            gid: 0,
            token: "".to_string(),
            title: title.to_string(),
            titleJpn: None,
            thumbKey: thumb
                .trim_start_matches(EHGT_PREFIX)
                .trim_start_matches(EX_PREFIX)
                .trim_start_matches("t/")
                .to_string(),
            category: to_category_i32(&category.trim().to_lowercase()),
            posted: "".to_string(),
            uploader: None,
            disowned: false,
            rating: parse_rating(rating),
            rated: false,
            simpleTags: vec![],
            pages: 0,
            thumbWidth: 0,
            thumbHeight: 0,
            spanSize: 0,
            spanIndex: 0,
            spanGroupIndex: 0,
            simpleLanguage: "".to_string(),
            favoriteSlot: 0,
            favoriteName: None,
        })
    })
    .unwrap()
    .into_java(&env)
    .forget()
    .into_raw()
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_: JavaVM, _: *mut c_void) -> jint {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Trace));
    JNI_VERSION_1_6
}
