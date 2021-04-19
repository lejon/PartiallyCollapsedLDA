import logging
import os
import shutil
import tempfile

from pclda.utils import check_output

logging.basicConfig(level=logging.DEBUG)


class PCLDA:
    def __init__(
        self,
        bin_path: str,  # path to PCPLDA JAR
        n_topics: int = 10,
        n_iterations: int = 200,
        alpha: float = 1.0,
        beta: float = 0.01,
        random_seed: int = -1,
        hyperparam_optim_interval: int = -1
    ) -> None:
        self.bin_path = bin_path
        self.base_out_dir = os.path.join(tempfile.gettempdir(), '.pclda')
        self.config_path = os.path.join(self.base_out_dir, 'pclda.cfg')

        self.config = dict(
            topics=n_topics,
            iterations=n_iterations,
            scheme='polyaurn',
            alpha=alpha,
            beta=beta,
            no_preprocess='true',
            save_doc_topic_means='true',
            doc_topic_mean_filename='doc_topic_means.csv',
            hyperparam_optim_interval=hyperparam_optim_interval,
            base_out_dir=self.base_out_dir,
            seed=random_seed,
            save_vocabulary='true',
            vocabulary_filename='vocab.txt',
            print_phi='true',
            start_diagnostic=n_iterations,
            rare_threshold=0,
            nr_top_words=20,
        )

    def fit(self, corpus: str) -> None:
        if str == type(corpus) and os.path.isfile(corpus):
            # assume mallet style corpus file on disk
            self.config['dataset'] = corpus

        # remove any previously cached contents
        if os.path.isdir(self.base_out_dir):
            shutil.rmtree(self.base_out_dir)

        os.makedirs(self.base_out_dir)

        self._config2file(self.config, self.config_path)

        cmd = f'java -jar {self.bin_path} --run_cfg {self.config_path}'
        check_output(cmd)

    def _config2file(self, config: dict, path: str) -> None:
        with open(path, 'w') as f:
            for key, val in config.items():
                f.write(f'{key} = {val}\n')
