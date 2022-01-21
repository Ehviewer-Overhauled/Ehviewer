/*
 * Copyright 2018 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "SevenZip.h"

#include <dlfcn.h>

#include <Windows/PropVariant.h>
#include <7zip/Archive/IArchive.h>
#include <7zip/IPassword.h>

#include "Log.h"
#include "Utils.h"

#include "OpenVolumeCallback.h"

#ifdef LOG_TAG
#  undef LOG_TAG
#  define LOG_TAG "P7Zip"
#endif

using namespace a7zip;

extern "C" HRESULT GetNumberOfMethods(UINT32 *numCodecs);
extern "C" HRESULT GetNumberOfFormats(UINT32 *numFormats);
extern "C" HRESULT GetMethodProperty(UInt32 codecIndex, PROPID propID, PROPVARIANT* value);
extern "C" HRESULT GetHandlerProperty2(UInt32 formatIndex, PROPID propID, PROPVARIANT* value);
extern "C" HRESULT CreateObject(const GUID* clsid, const GUID* iid, void** outObject);

typedef HRESULT (*GetPropertyFunc)(UInt32 index, PROPID propID, PROPVARIANT* value);

class Method {
 public:
  bool has_name;
  AString name;
  bool has_encoder;
  GUID encoder;
  bool has_decoder;
  GUID decoder;
};

class Format {
 public:
  GUID class_id;
  bool has_name;
  AString name;
  UInt32 signature_offset;
  CObjectVector<CByteBuffer> signatures;
};

class CompressCodecsInfo :
    public ICompressCodecsInfo,
    public CMyUnknownImp {
 public:
  MY_UNKNOWN_IMP1(ICompressCodecsInfo)
  STDMETHOD(GetNumMethods)(UInt32* numMethods);
  STDMETHOD(GetProperty)(UInt32 index, PROPID propID, PROPVARIANT* value);
  STDMETHOD(CreateDecoder)(UInt32 index, const GUID* interfaceID, void** coder);
  STDMETHOD(CreateEncoder)(UInt32 index, const GUID* interfaceID, void** coder);
};

static bool initialized = false;
static void* handle = nullptr;
static CObjectVector<Method> methods;
static CObjectVector<Format> formats;
static CompressCodecsInfo compress_codecs_info;

HRESULT CompressCodecsInfo::GetNumMethods(UInt32 *numMethods) {
  if (numMethods != nullptr) {
    *numMethods = methods.Size();
  }
  return S_OK;
}

HRESULT CompressCodecsInfo::GetProperty(UInt32 index, PROPID propID, PROPVARIANT* value) {
  Method& method = methods[index];

  switch (propID) {
    case NMethodPropID::kDecoderIsAssigned: {
      NWindows::NCOM::CPropVariant propVariant;
      propVariant = method.has_decoder;
      propVariant.Detach(value);
      return S_OK;
    }
    case NMethodPropID::kEncoderIsAssigned: {
      NWindows::NCOM::CPropVariant propVariant;
      propVariant = method.has_encoder;
      propVariant.Detach(value);
      return S_OK;
    }
    default: {
      return GetMethodProperty(index, propID, value);
    }
  }
}

HRESULT CompressCodecsInfo::CreateDecoder(
    UInt32 index,
    const GUID* interfaceID,
    void** coder
) {
  Method& method = methods[index];

  if (method.has_decoder) {
    return CreateObject(&(method.decoder), interfaceID, coder);
  } else {
    return S_OK;
  }
}

HRESULT CompressCodecsInfo::CreateEncoder(
    UInt32 index,
    const GUID* interfaceID,
    void** coder
) {
  Method& method = methods[index];

  if (method.has_encoder) {
    return CreateObject(&(method.encoder), interfaceID, coder);
  } else {
    return S_OK;
  }
}

class ArchiveOpenCallback :
    public IArchiveOpenCallback,
    public ICryptoGetTextPassword,
    public CMyUnknownImp {
 public:
  ArchiveOpenCallback(BSTR password) {
    this->password = ::SysAllocString(password);
    this->has_asked_password = false;
  }

  ~ArchiveOpenCallback() {
    ::SysFreeString(password);
  }

 public:
  MY_UNKNOWN_IMP2(IArchiveOpenCallback, ICryptoGetTextPassword)
  INTERFACE_IArchiveOpenCallback({ return S_OK; });

  STDMETHOD(CryptoGetTextPassword)(BSTR *password) {
    has_asked_password = true;
    *password = ::SysAllocString(this->password);
    return this->password != nullptr ? S_OK : E_NO_PASSWORD;
  }

  HRESULT GetBetterResult(HRESULT result) {
    if (result == S_OK) {
      return S_OK;
    } else if (has_asked_password) {
      if (password != nullptr) {
        return E_WRONG_PASSWORD;
      } else {
        return E_NO_PASSWORD;
      }
    } else {
      return result;
    }
  }

 private:
  BSTR password;
  bool has_asked_password;
};

class ArchiveOpenCallback2 :
    public ArchiveOpenCallback,
    public IArchiveOpenVolumeCallback {

 public:
  ArchiveOpenCallback2(
      BSTR password,
      BSTR filename,
      CMyComPtr<OpenVolumeCallback>& callback
  ):
      ArchiveOpenCallback(password),
      filename(filename),
      callback(callback) { }

 public:
  MY_UNKNOWN_IMP3(
      IArchiveOpenCallback,
      IArchiveOpenVolumeCallback,
      ICryptoGetTextPassword)

  STDMETHOD(GetProperty)(PROPID propID, PROPVARIANT *value) {
    NWindows::NCOM::CPropVariant prop;
    if (propID == kpidName) {
      prop = filename;
    }
    prop.Detach(value);
    return S_OK;
  }

  STDMETHOD(GetStream)(const wchar_t *name, IInStream **inStream) {
    CMyComPtr<IInStream> stream = nullptr;
    HRESULT result = callback->OpenVolume(name, stream);
    *inStream = stream.Detach();
    return result != S_OK ? S_FALSE : S_OK;
  }

 private:
  UString filename;
  CMyComPtr<OpenVolumeCallback> callback;
};

#define GET_PROP_METHOD(METHOD_NAME, PROP_TYPE, VALUE_TYPE, CONVERTER)         \
HRESULT METHOD_NAME(                                                           \
    GetPropertyFunc get_property,                                              \
    UInt32 index,                                                              \
    PROPID prop_id,                                                            \
    PROP_TYPE& value,                                                          \
    bool& is_assigned                                                          \
) {                                                                            \
  NWindows::NCOM::CPropVariant prop;                                           \
  is_assigned = false;                                                         \
                                                                               \
  RETURN_SAME_IF_NOT_ZERO(get_property(index, prop_id, &prop));                \
                                                                               \
  if (prop.vt == VALUE_TYPE) {                                                 \
    is_assigned = true;                                                        \
    CONVERTER;                                                                 \
  } else if (prop.vt != VT_EMPTY) {                                            \
    return E_INCONSISTENT_PROP_TYPE;                                           \
  }                                                                            \
                                                                               \
  return S_OK;                                                                 \
}

GET_PROP_METHOD(GetAString, AString, VT_BSTR, value.SetFromWStr_if_Ascii(prop.bstrVal));

GET_PROP_METHOD(GetGUID, GUID, VT_BSTR, value = *reinterpret_cast<const GUID*>(prop.bstrVal));

GET_PROP_METHOD(GetCByteBuffer, CByteBuffer, VT_BSTR, value.CopyFrom((const Byte *)prop.bstrVal, ::SysStringByteLen(prop.bstrVal)));

GET_PROP_METHOD(GetUInt32, UInt32, VT_UI4, value = prop.ulVal);

#undef GET_PROP_METHOD

static HRESULT LoadMethods() {
  UInt32 method_number = 0;

  RETURN_SAME_IF_NOT_ZERO(GetNumberOfMethods(&method_number));

  for(UInt32 i = 0; i < method_number; i++) {
    Method& method = methods.AddNew();

#   define GET_PROP(METHOD, PROP, VALUE, ASSIGNED)                              \
    if (METHOD(GetMethodProperty, i, PROP, VALUE, ASSIGNED) != S_OK) {          \
      methods.DeleteBack();                                                     \
      continue;                                                                 \
    }

    // It's ok to assume all characters in method name are in ascii charset
    GET_PROP(GetAString, NMethodPropID::kName, method.name, method.has_name);
    GET_PROP(GetGUID, NMethodPropID::kDecoder, method.decoder, method.has_decoder);
    GET_PROP(GetGUID, NMethodPropID::kEncoder, method.encoder, method.has_encoder);

#   undef GET_PROP
  }

  return S_OK;
}

static void AppendMultiSignature(
    CObjectVector<CByteBuffer>& signatures,
    const unsigned char* multi_signature,
    size_t size
) {
  while (size > 0) {
    unsigned length = *multi_signature++;
    size--;

    if (length > size) {
      return;
    }

    signatures.AddNew().CopyFrom(multi_signature, length);

    multi_signature += length;
    size -= length;
  }
}

static HRESULT LoadFormats() {
  UInt32 format_number = 0;

  RETURN_SAME_IF_NOT_ZERO(GetNumberOfFormats(&format_number));

  for(UInt32 i = 0; i < format_number; i++) {
    Format& format = formats.AddNew();

#   define GET_PROP(METHOD, PROP, VALUE, ASSIGNED)                              \
    if (METHOD(GetHandlerProperty2, i, PROP, VALUE, ASSIGNED) != S_OK) {        \
      formats.DeleteBack();                                                     \
      continue;                                                                 \
    }

    // ClassID is required
    bool has_class_id;
    GET_PROP(GetGUID, NArchive::NHandlerPropID::kClassID, format.class_id, has_class_id);
    if (!has_class_id) {
      formats.DeleteBack();
      continue;
    }

    // It's ok to assume all characters in format name are in ascii charset
    GET_PROP(GetAString, NArchive::NHandlerPropID::kName, format.name, format.has_name);

    bool has_signature;
    CByteBuffer signature;
    GET_PROP(GetUInt32, NArchive::NHandlerPropID::kSignatureOffset, format.signature_offset, has_signature);
    if (!has_signature) {
      format.signature_offset = 0;
    }
    GET_PROP(GetCByteBuffer, NArchive::NHandlerPropID::kSignature, signature, has_signature);
    if (has_signature) {
      format.signatures.AddNew().CopyFrom(signature, signature.Size());
    }
    GET_PROP(GetCByteBuffer, NArchive::NHandlerPropID::kMultiSignature, signature, has_signature);
    if (has_signature) {
      AppendMultiSignature(format.signatures, signature, signature.Size());
    }

#   undef GET_PROP
  }

  return S_OK;
}

HRESULT SevenZip::Initialize() {
  if (initialized) {
    return S_OK;
  }

  RETURN_SAME_IF_NOT_ZERO(LoadMethods());
  RETURN_SAME_IF_NOT_ZERO(LoadFormats());

  initialized = true;
  return S_OK;
}

static HRESULT ReadFully(CMyComPtr<IInStream>& stream, Byte* data, UInt32 size, UInt32* processedSize) {
  UInt32 read = 0;

  while (read < size) {
    RETURN_SAME_IF_NOT_ZERO(stream->Read(data + read, size - read, processedSize));

    if (*processedSize == 0) {
      // EOF
      break;
    }

    read += *processedSize;
  }

  *processedSize = read;
  return S_OK;
}

static HRESULT OpenInArchive(
    GUID& class_id,
    CMyComPtr<IInStream>& in_stream,
    BSTR password,
    BSTR filename,
    CMyComPtr<OpenVolumeCallback>& open_volume_callback,
    CMyComPtr<IInArchive>& in_archive
) {
  RETURN_SAME_IF_NOT_ZERO(CreateObject(&class_id, &IID_IInArchive, reinterpret_cast<void **>(&in_archive)));

  UInt64 newPosition = 0;
  HRESULT result = in_stream->Seek(0, STREAM_SEEK_SET, &newPosition);
  if (result != S_OK) {
    in_archive->Close();
    in_archive = nullptr;
    return result;
  }

  UInt64 maxCheckStartPosition = 1 << 22;

  ArchiveOpenCallback* callback_ptr = nullptr;
  if (filename != nullptr && open_volume_callback != nullptr) {
    callback_ptr = new ArchiveOpenCallback2(password, filename, open_volume_callback);
  } else {
    callback_ptr = new ArchiveOpenCallback(password);
  }
  CMyComPtr<ArchiveOpenCallback> callback(callback_ptr);

  result = in_archive->Open(in_stream, &maxCheckStartPosition, callback);
  result = callback->GetBetterResult(result);
  if (result != S_OK) {
    in_archive->Close();
    in_archive = nullptr;
    return result;
  }

  return S_OK;
}

static HRESULT OpenInArchive(
    CMyComPtr<IInStream>& in_stream,
    BSTR password,
    BSTR filename,
    CMyComPtr<OpenVolumeCallback>& open_volume_callback,
    CMyComPtr<IInArchive>& in_archive,
    AString& format_name
) {
  bool formats_checked[formats.Size()];
  memset(formats_checked, 0, formats.Size() * sizeof(bool));

  for (int i = 0; i < formats.Size(); i++) {
    Format& format = formats[i];

    // Skip format without signatures
    if (format.signatures.Size() == 0) {
      continue;
    }

    // Check each signature
    for (int j = 0; j < format.signatures.Size(); j++) {
      CByteBuffer& signature = format.signatures[j];

      UInt32 processedSize;
      CByteBuffer bytes(signature.Size());

      UInt64 newPosition;
      CONTINUE_IF_NOT_ZERO(in_stream->Seek(format.signature_offset, STREAM_SEEK_SET, &newPosition));
      if (newPosition != format.signature_offset) continue;
      CONTINUE_IF_NOT_ZERO(ReadFully(in_stream, bytes, static_cast<UInt32>(signature.Size()), &processedSize));
      if (processedSize != signature.Size() || bytes != signature) continue;

      // The signature matched, try to open it
      HRESULT result = OpenInArchive(format.class_id, in_stream, password, filename, open_volume_callback, in_archive);

      // Mark the format
      formats_checked[i] = true;

      if (result == S_OK) {
        format_name = format.name;
        return S_OK;
      }

      if (result == E_NO_PASSWORD || result == E_WRONG_PASSWORD) {
        // It's a password error, the archive format is confirmed
        return result;
      }

      // Can't open archive in this format
      // Skip this format
      break;
    }
  }

  // Try other unchecked formats
  for (int i = 0; i < formats.Size(); i++) {
    if (!formats_checked[i]) {
      Format& format = formats[i];
      HRESULT result = OpenInArchive(format.class_id, in_stream, password, filename, open_volume_callback, in_archive);

      if (result == S_OK) {
        format_name = format.name;
        return S_OK;
      }

      if (result == E_NO_PASSWORD || result == E_WRONG_PASSWORD) {
        // It's a password error, the archive format is confirmed
        return result;
      }
    }
  }

  return E_UNKNOWN_FORMAT;
}

HRESULT SevenZip::OpenArchive(
    CMyComPtr<IInStream>& in_stream,
    BSTR password,
    BSTR filename,
    CMyComPtr<OpenVolumeCallback>& open_volume_callback,
    InArchive** archive
) {
  HRESULT result = S_FALSE;
  InArchive* previous_archive = nullptr;

  CMyComPtr<IInStream> arg_in_stream = in_stream;
  BSTR arg_password = password;
  BSTR arg_filename = filename;
  CMyComPtr<OpenVolumeCallback> arg_open_volume_callback = open_volume_callback;

  while (true) {
    CMyComPtr<IInArchive> in_archive = nullptr;
    AString format_name;

    result = OpenInArchive(
        arg_in_stream,
        arg_password,
        arg_filename,
        arg_open_volume_callback,
        in_archive,
        format_name
    );

    if (result != S_OK || in_archive == nullptr) {
      if (in_archive != nullptr) {
        in_archive->Close();
        in_archive = nullptr;
      }
      if (result == S_OK) {
        result = E_INTERNAL;
      }
      break;
    }

    previous_archive = new InArchive(previous_archive, in_archive, format_name);

    // Break if it has no entry or more than one entry
    UInt32 number = 0;
    previous_archive->GetNumberOfEntries(number);
    if (number != 1) {
      break;
    }

    // Break if it's not IInArchiveGetStream
    CMyComPtr<IInArchiveGetStream> archive_get_stream;
    in_archive->QueryInterface(IID_IInArchiveGetStream, (void**)&archive_get_stream);
    if (archive_get_stream == nullptr) {
      break;
    }

    // Break if GetStream returns null
    CMyComPtr<ISequentialInStream> sequential_in_stream;
    archive_get_stream->GetStream(0, &sequential_in_stream);
    if (sequential_in_stream == nullptr) {
      break;
    }

    // Break if the stream is not IInStream
    arg_in_stream = nullptr;
    sequential_in_stream->QueryInterface(IID_IInStream, (void**)&arg_in_stream);
    if (arg_in_stream == nullptr) {
      break;
    }

    // Clear arguments for the first archive
    arg_password = nullptr;
    arg_filename = nullptr;
    arg_open_volume_callback = nullptr;
  }

  *archive = previous_archive;
  return *archive != nullptr ? S_OK : result;
}
