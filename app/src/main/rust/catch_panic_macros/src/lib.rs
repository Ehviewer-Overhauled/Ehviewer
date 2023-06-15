use std::ops::Deref;

use proc_macro::TokenStream;
use quote::{quote, quote_spanned, ToTokens};
use syn::spanned::Spanned;
use syn::{
    parse_macro_input, AttributeArgs, Expr, FnArg, ItemFn, Lit, Meta, MetaNameValue, NestedMeta,
    Pat, Path,
};

const ONLY_FUNCTIONS_MSG: &str = "#[catch_panic] can only be applied to functions";
const FIRST_ARG_MSG: &str =
    "#[catch_panic] requires that this function has at least one argument of type jni::JNIEnv";
const INVALID_NAME_MSG: &str = "Invalid function parameter name";

/// Aborts parsing with the given error.
/// This will also emit the original item as-is to suppress red herring unused warnings.
macro_rules! abort {
    ($err:expr, $item:expr $(,)?) => {{
        let err = $err;
        let item = $item;
        return TokenStream::from(quote! {
            #err
            #item
        });
    }};
}

/// Catches a panic and rethrows it as a Java exception.
/// See the [crate-level documentation](../catch_panic/index.html) for more information.
#[proc_macro_attribute]
pub fn catch_panic(attr: TokenStream, item: TokenStream) -> TokenStream {
    let item = proc_macro2::TokenStream::from(item);

    // parse macro arguments
    let args = parse_macro_input!(attr as AttributeArgs);
    let mut default_value = None;
    let mut handler = None;

    for arg in args {
        if let NestedMeta::Meta(Meta::NameValue(MetaNameValue {
            path,
            lit: Lit::Str(str),
            ..
        })) = arg
        {
            match path.get_ident() {
                Some(id) if id == "default" => match str.parse::<Expr>() {
                    Ok(expr) => default_value = Some(expr),
                    Err(err) => abort!(err.to_compile_error(), item),
                },
                Some(id) if id == "handler" => match str.parse::<Path>() {
                    Ok(expr) => handler = Some(expr),
                    Err(err) => abort!(err.to_compile_error(), item),
                },
                _ => {}
            }
        }
    }

    let default_value = match default_value {
        Some(val) => val.to_token_stream(),
        None => quote! { ::std::default::Default::default() },
    };
    let handler = match handler {
        Some(val) => val.to_token_stream(),
        None => quote! { ::catch_panic::handler::default_handler },
    };

    // parse and validate function
    let item_span = item.span();
    let (attrs, vis, sig, block) = match syn::parse2::<ItemFn>(item.clone()) {
        Ok(ItemFn {
            attrs,
            vis,
            sig,
            block,
        }) => (attrs, vis, sig, block),
        Err(_) => abort!(
            quote_spanned! {item_span=>
                compile_error!(#ONLY_FUNCTIONS_MSG);
            },
            item,
        ),
    };
    if sig.inputs.is_empty() {
        abort!(
            quote_spanned! {sig.span()=>
                compile_error!(#FIRST_ARG_MSG);
            },
            item,
        );
    }
    let first_arg_name = match sig.inputs.first().unwrap() {
        FnArg::Receiver(receiver) => abort!(
            quote_spanned! {receiver.span()=>
                compile_error!(#FIRST_ARG_MSG);
            },
            item,
        ),
        FnArg::Typed(pat_type) => match pat_type.pat.deref() {
            Pat::Ident(pat) => pat.ident.clone(),
            _ => abort!(
                quote_spanned! {pat_type.span()=>
                    compile_error!(#INVALID_NAME_MSG);
                },
                item,
            ),
        },
    };

    TokenStream::from(quote! {
        #(#attrs)*
        #vis #sig {
            ::catch_panic::handler::__catch_panic(#first_arg_name, #default_value, #handler, move || {
                #block
            })
        }
    })
}
