use catch_panic::catch_panic;
use html_escape::decode_html_entities;
use jni_fn::jni_fn;
use jnix::jni::objects::{JClass, JString};
use jnix::jni::sys::jobject;
use jnix::jni::JNIEnv;
use jnix::{IntoJava, JnixEnv};
use jnix_macros::IntoJava;
use {parse_jni_string, regex};

#[derive(Default, IntoJava)]
#[jnix(package = "com.hippo.ehviewer.client.parser")]
pub struct Torrent {
    posted: String,
    size: String,
    seeds: i32,
    peers: i32,
    downloads: i32,
    url: String,
    name: String,
}

#[derive(Default, IntoJava)]
#[jnix(package = "com.hippo.ehviewer.client.parser")]
pub struct TorrentResult {
    list: Vec<Torrent>,
}

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.TorrentParserKt")]
pub fn parseTorrent(env: JNIEnv, _class: JClass, input: JString) -> jobject {
    let mut env = JnixEnv { env };
    parse_jni_string(&mut env, &input, |dom, parser, _env| {
        Some(TorrentResult {
            list: dom.query_selector("table")?.filter_map(|e| {
                let html = e.get(parser)?.inner_html(parser);
                let reg = regex!("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9.]+ [KMGT]B)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>");
                let grp = reg.captures(&html)?;
                let name = decode_html_entities(&grp[8]);
                Some(Torrent {
                    posted: grp[1].to_string(),
                    size: grp[2].to_string(),
                    seeds: grp[3].parse().ok()?,
                    peers: grp[4].parse().ok()?,
                    downloads: grp[5].parse().ok()?,
                    url: grp[7].to_string(),
                    name: name.to_string()
                })
            }).collect()
        })
    }).unwrap().into_java(&env).forget().into_raw()
}
