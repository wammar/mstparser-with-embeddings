#!/usr/bin/env ducttape

global {
  ducttape_experimental_submitters=enable
  ducttape_experimental_imports=enable
  ducttape_output="/usr3/home/wammar/mstparsers/temp"

  wang2vec_root="/usr0/home/wammar/git/wang2vec/"
  plain_text="/usr3/home/wammar/corpora/monolingual/plain-en/news-commentary-v8.en.tok"
  #treebank_conll="/usr3/home/wammar/corpora/universal-dependencies-1.0/treebanks/en/en-ud-train.conllu"
  treebank_conll="/usr3/home/wammar/corpora/universal-dependencies-1.0/treebanks/en/en-ud-test.conllu"
  test_conll="/usr3/home/wammar/corpora/universal-dependencies-1.0/treebanks/en/en-ud-dev.conllu"
  wammar_utils_root="/usr0/home/wammar/wammar-utils/"
  parser_root="/usr0/home/wammar/mstparser-with-embeddings/mstparser/"
  eacl14_cca_root="/usr0/home/wammar/git/eacl14-cca/"
  ignore_cpos_tags=(IgnoreCposTags: no="" yes="yes")
  use_clusters=(UseClusters: no="" yes="yes")
  l2=(L2: one="1" three="3" ten="10")
}

task LearnEmbeddings
    :: wang2vec_root=@
    :: plain_text=@
    :: treebank_conll=@
    :: wammar_utils_root=@
    > embeddings
{
  python $wammar_utils_root/convert-conll-to-text.py -i $treebank_conll -o treebank_text
  cat $plain_text treebank_text > all_text
  python $wammar_utils_root/lowercase.py -i all_text -o lower_text
  $wang2vec_root/word2vec -min-count 1 -type 3 -train lower_text -output $embeddings
  rm treebank_text all_text lower_text
}

task ClusterEmbeddings
    :: eacl14_cca_root=@
    > clusters
    < embeddings=$embeddings@LearnEmbeddings
{
  pushd $eacl14_cca_root
  python $eacl14_cca_root/cluster_embeddings.py -i $embeddings -o $clusters
  popd
}

task BuildParser
    :: parser_root=@
    > parser_bin_dir
{
  pushd $parser_root
  sh build.sh
  popd
  ln -s $parser_root/bin $parser_bin_dir
}

task TrainParser
 :: use_clusters=@
 :: ignore_cpos_tags=@
 < parser_bin_dir=$parser_bin_dir@BuildParser
 < clusters=$clusters@ClusterEmbeddings
 < treebank_conll=@
 < test_conll=@
 > predictions
 > model
{
  export clusters_option=""
  if [ $use_clusters ]; then
    export clusters_option="word-clusters-file:$clusters"
  fi

  export cpos_tags_option=""
  if [ $ignore_cpos_tags ]; then
    export cpos_tags_option="ignore-cpos-tags"
  fi
 
  pushd $parser_bin_dir
  ./mst_parse.sh train train-file:$treebank_conll model-name:$model test test-file:$test_conll output-file:$predictions eval gold-file:$test_conll $clusters_option $cpos_tags_option ignore-pos-tags ignore-lemmas ignore-morphology ignore-surface-forms
  popd
}

plan Full {
    reach TrainParser via (UseClusters: no) * (IgnoreCposTags: yes)
}
