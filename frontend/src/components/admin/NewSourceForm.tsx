"use client";

import { useState } from "react";

import { postCreateSource } from "@/app/admin/sources/actions";
import { SourceForm } from "@/components/admin/SourceForm";

export function NewSourceForm() {
  // 성공 후 폼을 리마운트시켜 입력값을 비움
  const [formKey, setFormKey] = useState(0);

  return (
    <SourceForm
      key={formKey}
      submitLabel="추가"
      onSubmit={postCreateSource}
      onSuccess={() => setFormKey((k) => k + 1)}
    />
  );
}
