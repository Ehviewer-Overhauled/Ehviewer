use catch_panic::catch_panic;
use html_escape::decode_html_entities;
use jni_fn::jni_fn;
use jnix::jni::objects::{JClass, JString};
use jnix::jni::sys::jobject;
use jnix::jni::JNIEnv;
use jnix::{IntoJava, JnixEnv};
use jnix_macros::IntoJava;
use query_childs_first_match_attr;
use std::borrow::Cow;
use {get_node_handle_attr, regex};
use {parse_jni_string, Anon};
use {EHGT_PREFIX, EX_PREFIX};

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
    simpleLanguage: String,
    favoriteSlot: i32,
    favoriteName: Option<String>,
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
    let mut next = || {
        Some(
            iter.next()?
                .as_str()
                .replace("px", "")
                .parse::<i32>()
                .ok()?,
        )
    };
    match (next(), next()) {
        (Some(num1), Some(num2)) => {
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

fn get_thumb_key(url: &str) -> String {
    url.trim_start_matches(EHGT_PREFIX)
        .trim_start_matches(EX_PREFIX)
        .trim_start_matches("t/")
        .to_string()
}

fn parse_token_and_gid(str: &str) -> (i64, String) {
    let reg = regex!(
        "https?://(?:exhentai.org|e-hentai.org|lofi.e-hentai.org)/(?:g|mpv)/(\\d+)/([0-9a-f]{10})"
    );
    let grp = reg.captures(str).unwrap();
    let token = &grp[2];
    let gid = grp[1].parse().unwrap();
    (gid, token.to_string())
}

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.GalleryListParserKt")]
pub fn parseGalleryInfo(env: JNIEnv, _class: JClass, input: JString) -> jobject {
    let mut env = JnixEnv { env };
    parse_jni_string(&mut env, &input, |dom, parser, _env| {
        let title = match dom.get_first_element_by_class_name("glink") {
            None => panic!("No title found"),
            Some(glink) => glink.inner_text(parser),
        };
        let gdlink = match dom.get_first_element_by_class_name("glname") {
            None => panic!("Cannot parse token and gid!"),
            Some(glname) => match query_childs_first_match_attr(glname, parser, "href") {
                None => query_childs_first_match_attr(&dom.nodes()[0], parser, "href")?,
                Some(attr) => attr,
            },
        };
        let (gid, token) = parse_token_and_gid(gdlink);
        let thumb = match dom.query_selector("[data-src]")?.next() {
            None => match dom.query_selector("[src]")?.next() {
                None => panic!("No thumb found"),
                Some(thumb) => get_node_handle_attr(&thumb, parser, "src")?,
            },
            Some(thumb) => get_node_handle_attr(&thumb, parser, "data-src")?,
        };
        let category = match dom.get_first_element_by_class_name("cn") {
            None => match dom.get_first_element_by_class_name("cs") {
                None => Cow::from("unknown"),
                Some(cs) => cs.inner_text(parser),
            },
            Some(cn) => cn.inner_text(parser),
        };
        let ir = dom.get_first_element_by_class_name("ir")?;
        let rating = ir.as_tag()?.attributes().get("style")??.try_as_utf8_str()?;
        Some(BaseGalleryInfo {
            gid,
            token,
            title: decode_html_entities(title.trim()).to_string(),
            titleJpn: None,
            thumbKey: get_thumb_key(thumb),
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
