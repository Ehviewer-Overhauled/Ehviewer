use catch_panic::catch_panic;
use jni_fn::jni_fn;
use jnix::jni::objects::{JByteBuffer, JClass};
use jnix::jni::sys::{jint, jobject};
use jnix::jni::JNIEnv;
use jnix::{IntoJava, JnixEnv};
use jnix_macros::IntoJava;
use parse_bytebuffer;
use quick_xml::escape::unescape;
use regex;

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

#[no_mangle]
#[catch_panic(default = "std::ptr::null_mut()")]
#[allow(non_snake_case)]
#[jni_fn("com.hippo.ehviewer.client.parser.TorrentParserKt")]
pub fn parseTorrent(env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) -> jobject {
    let mut env = JnixEnv { env };
    parse_bytebuffer(&mut env, buffer, limit, |dom, parser, _env| {
        Some(dom.query_selector("table")?.filter_map(|e| {
            let html = e.get(parser)?.inner_html(parser);
            let grp = regex!("</span> ([0-9-]+) [0-9:]+</td>[\\s\\S]+</span> ([0-9.]+ [KMGT]B)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span> ([0-9]+)</td>[\\s\\S]+</span>([^<]+)</td>[\\s\\S]+onclick=\"document.location='([^\"]+)'[^<]+>([^<]+)</a>").captures(&html)?;
            let name = unescape(&grp[8]).ok()?;
            Some(Torrent {
                posted: grp[1].to_string(),
                size: grp[2].to_string(),
                seeds: grp[3].parse().ok()?,
                peers: grp[4].parse().ok()?,
                downloads: grp[5].parse().ok()?,
                url: grp[7].to_string(),
                name: name.to_string()
            })
        }).collect::<Vec<Torrent>>())
    }).unwrap().into_java(&env).forget().into_raw()
}
